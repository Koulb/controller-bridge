package app.controllerbridge;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.view.InputDevice;
import io.github.muntashirakon.adb.AdbStream;
import io.github.muntashirakon.adb.android.AdbMdns;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public final class BridgeService extends Service {
    public static final String ON="app.controllerbridge.ON", OFF="app.controllerbridge.OFF",
        PAIR="app.controllerbridge.PAIR", CODE="app.controllerbridge.CODE", CONNECT="app.controllerbridge.CONNECT", EXIT="app.controllerbridge.EXIT", TEST="app.controllerbridge.TEST";
    public static volatile boolean active, busy, pairing;
    public static volatile String message="Set up phone access to begin", ownDescriptor="";
    private static final String CHANNEL="controller", CODE_KEY="code";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService timers=Executors.newSingleThreadScheduledExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Object lock=new Object();
    private volatile long generation;
    private volatile long latestOnGeneration=-1;
    private volatile boolean destroyed;
    private volatile AdbStream stream, query;
    private volatile PhoneAdb manager;
    private volatile Future<?> operation;
    private AdbMdns mdns;
    private volatile int pairingPort=-1;
    private final Runnable health=new Runnable(){ public void run(){
        if(active) {
            if(!HelperConnection.available() || !hasOwnDevice()) {
                endVirtual(); message="Virtual controller disconnected. Tap Turn on to reconnect.";
            } else if(physical(BridgeService.this).isEmpty()) {
                endVirtual(); message="Controller disconnected. Virtual DualSense stopped.";
            }
        }
        if(!active && !busy && !pairing && !HelperConnection.available()) stopForeground(STOP_FOREGROUND_REMOVE);
        main.postDelayed(this,1000);
    }};

    public static List<ControllerRules.Pad> physical(Context c) {
        String own=ownDescriptor.isEmpty()?c.getSharedPreferences("bridge",0).getString("descriptor",""):ownDescriptor;
        List<ControllerRules.Pad> pads=new ArrayList<>();
        for(int id:InputDevice.getDeviceIds()) {
            InputDevice d=InputDevice.getDevice(id);
            if(d!=null) pads.add(new ControllerRules.Pad(d.getName(),d.getVendorId(),d.getProductId(),d.getSources(),d.isVirtual(),d.getDescriptor()));
        }
        return ControllerRules.physical(pads,own);
    }
    private boolean hasOwnDevice() {
        for(int id:InputDevice.getDeviceIds()) {
            InputDevice d=InputDevice.getDevice(id);
            if(d!=null && !ownDescriptor.isEmpty() && ownDescriptor.equals(d.getDescriptor()) && d.getVendorId()==0x054c && d.getProductId()==0x0ce6) return true;
        }
        return false;
    }
    @Override public void onCreate() {
        super.onCreate(); active=false; busy=false; pairing=false;
        ownDescriptor=getSharedPreferences("bridge",0).getString("descriptor","");
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"Controller access",NotificationManager.IMPORTANCE_DEFAULT));
        main.post(health);
        timers.scheduleWithFixedDelay(()->{if(HelperConnection.available())try{HelperConnection.call(HelperConnection.STATUS);}catch(Exception ignored){}},2,2,TimeUnit.SECONDS);
    }
    @Override public IBinder onBind(Intent i){return null;}
    private Notification notification(String text,boolean withCode) {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_controller).setContentTitle("Controller Bridge").setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true);
        if(withCode) {
            PendingIntent reply=PendingIntent.getService(this,1,new Intent(this,BridgeService.class).setAction(CODE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);
            b.addAction(new Notification.Action.Builder(null,"Enter pairing code",reply).addRemoteInput(new RemoteInput.Builder(CODE_KEY).setLabel("6-digit pairing code").build()).build());
        }
        PendingIntent stop=PendingIntent.getService(this,2,new Intent(this,BridgeService.class).setAction(OFF),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        b.addAction(new Notification.Action.Builder(null,(withCode||busy)?"Cancel":"Turn off",stop).build());
        return b.build();
    }
    private void foreground(String text,boolean code) {
        if(!destroyed) startForeground(1,notification(text,code),ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
    }
    private boolean valid(long token){return !destroyed && generation==token;}
    private void check(long token)throws IOException {if(!valid(token))throw new IOException("Operation cancelled");}
    private static void close(AdbStream s) {
        if(s==null)return;
        // ADB close sends a network packet; Android forbids doing this on the UI thread.
        Thread closer=new Thread(()->{try{s.close();}catch(Exception e){android.util.Log.w("ControllerBridge","Stream cleanup failed",e);}},"adb-close");
        closer.setDaemon(true);closer.start();
    }
    private boolean cancel(String reason) {
        final boolean revoked;
        synchronized(lock) {generation++; revoked=ActivationRequest.cancel(this); busy=false; endVirtual(); close(query); query=null; message=revoked?reason:"Could not save activation cancellation. Access may still start for up to 60 seconds. Use Stop phone access after the app reopens.";}
        Future<?> pending=operation;if(pending!=null)pending.cancel(true);
        PhoneAdb adb=manager; if(adb!=null) adb.cancelPairing();
        stopPairing();if(HelperConnection.available())foreground("Offline phone access ready",false);else stopForeground(STOP_FOREGROUND_REMOVE);
        return revoked;
    }
    @Override public int onStartCommand(Intent i,int flags,int startId) {
        if(i==null)return START_NOT_STICKY;
        String action=i.getAction();
        if(EXIT.equals(action)){
            if(!cancel("Stopping phone access…")&&!HelperConnection.available())return START_NOT_STICKY;
            final IBinder old=HelperConnection.detach();
            busy=true;foreground("Stopping phone access…",false);
            worker.execute(()->{try{HelperConnection.call(old,HelperConnection.EXIT);}catch(Exception ignored){}finally{main.post(()->{busy=false;message="Phone access stopped. Activate again to use it.";stopForeground(STOP_FOREGROUND_REMOVE);});}});
            return START_NOT_STICKY;
        }
        if(OFF.equals(action)){cancel("Virtual DualSense is off");return START_NOT_STICKY;}
        // Satisfy startForegroundService even if a duplicate request arrives.
        foreground(active?"Virtual DualSense on":"Preparing phone access…",pairing);
        if(busy)return START_NOT_STICKY;
        if(PAIR.equals(action)){startPairing();return START_NOT_STICKY;}
        busy=true;
        final long token;
        synchronized(lock){token=++generation;if(ON.equals(action))latestOnGeneration=token;}
        final Bundle reply=RemoteInput.getResultsFromIntent(i);
        Runnable timeout=()->{if(valid(token)&&busy)cancel("Phone access timed out. Try pairing or connecting again.");};
        main.postDelayed(timeout,50000);
        FutureTask<Void> task=new FutureTask<>(()->{
            try {
                check(token);
                if(HelperConnection.available() && !CODE.equals(action)) {
                    if(ON.equals(action))startVirtual(token);
                    else if(TEST.equals(action))selfTest(token);
                    else message="Offline phone access ready. Wi-Fi debugging can stay off.";
                } else {
                PhoneAdb adb=manager;if(adb==null){adb=PhoneAdb.create(this);manager=adb;}check(token);
                if(CODE.equals(action)) {
                    String code=reply==null?"":String.valueOf(reply.getCharSequence(CODE_KEY,""));
                    if(!code.matches("[0-9]{6}"))throw new IOException("Enter the six digits shown by Android.");
                    if(pairingPort<1)throw new IOException("Open Wireless debugging → Pair device with pairing code first.");
                    adb.pairLocal(pairingPort,code,()->valid(token)); check(token);
                    main.post(()->{if(valid(token))stopPairing();});
                    message="Paired. Connecting to this phone…";
                }
                check(token);
                if(!adb.isConnected()) {
                    int port=i.getIntExtra("port",0);
                    boolean connected=port>0?adb.connect("127.0.0.1",port):adb.connectTls(this,12000);
                    check(token);
                    if(!connected&&!adb.isConnected())throw new IOException("Enable Wireless debugging, then tap Connect. Use the connection port if discovery fails.");
                }
                check(token);
                activateHelper(adb,token);
                synchronized(lock){check(token);message="Restarting app to activate input access…";}
                }
            } catch(Exception e) {
                synchronized(lock){if(valid(token)){if(ON.equals(action))endVirtual();message=e.getMessage()==null?"Could not connect. Check Wireless debugging and pairing.":e.getMessage();}}
                android.util.Log.e("ControllerBridge","Operation failed",e);
            } finally {
                main.removeCallbacks(timeout);
                // Interrupting openStream wakes its unbounded OPEN-response wait.
                // Disconnect on this serial worker before any subsequent connection attempt.
                if(!valid(token)&&manager!=null)try{manager.disconnect();}catch(IOException ignored){}
                main.post(()->{if(!valid(token))return;busy=false;if(active)foreground("Virtual DualSense on",false);else if(pairing)foreground(message,true);else if(HelperConnection.available())foreground("Offline phone access ready",false);else stopForeground(STOP_FOREGROUND_REMOVE);});
            }
        },null);
        operation=task;worker.execute(task);
        return START_NOT_STICKY;
    }
    private void startPairing() {
        if(pairing)return;
        pairing=true; pairingPort=-1;
        message="Open Wireless debugging → Pair device with pairing code. Enter code in this app’s notification.";
        mdns=new AdbMdns(this,AdbMdns.SERVICE_TYPE_TLS_PAIRING,(host,port)->{
            if(host!=null&&port>0)main.post(()->{if(pairing){pairingPort=port;foreground("Pairing found. Enter Android’s 6-digit code.",true);}});
        });
        mdns.start(); foreground("Open Android pairing screen, then enter code here",true);
    }
    private void stopPairing(){pairing=false;pairingPort=-1;if(mdns!=null){mdns.stop();mdns=null;}}
    private static String quote(String value){return "'"+value.replace("'","'\"'\"'")+"'";}
    private void activateHelper(PhoneAdb adb,long token)throws Exception {
        if(HelperConnection.available())return;
        if(Build.VERSION.SDK_INT<37)throw new IOException("This experimental input API requires Android 17 or later.");
        // Detach before instrumentation restarts this app and closes its ADB client.
        // The temporary shell disables wireless debugging only after the new app
        // confirms permission delegation. It is not needed for subsequent controls.
        final String nonce;
        synchronized(lock){check(token);nonce=ActivationRequest.arm(this);}
        String script="/system/bin/am instrument -w --no-hidden-api-checks -e activation_nonce "+quote(nonce)+" app.controllerbridge/.ControllerInstrumentation | while IFS= read -r line; do case \"$line\" in 'INSTRUMENTATION_STATUS: controller_bridge_ready=1') /system/bin/settings put global adb_wifi_enabled 0;; esac; done";
        String command="/system/bin/setsid /system/bin/sh -c "+quote(script)+" </dev/null >/dev/null 2>&1 &";
        try {
            check(token);
            AdbStream launch=adb.openStream("shell,v2,raw:"+command);
            synchronized(lock){if(!valid(token)){close(launch);check(token);}query=launch;}
            ScheduledFuture<?> expiry=timers.schedule(()->close(launch),20,TimeUnit.SECONDS);
            try{readAll(launch.openInputStream(),4096);}finally{expiry.cancel(false);close(launch);synchronized(lock){if(query==launch)query=null;}}
        } finally {adb.disconnect();}
    }
    private void startVirtual(long token)throws Exception {
        if(active)return;
        List<ControllerRules.Pad> pads=physical(this);
        if(pads.isEmpty())throw new IOException("Connect a gamepad first.");
        for(ControllerRules.Pad p:pads)if(ControllerRules.isDualSense(p))throw new IOException("DualSense already connected. No virtual controller needed.");
        check(token);Bundle result=HelperConnection.call(HelperConnection.ON);
        if(!valid(token)){HelperConnection.call(HelperConnection.OFF);check(token);}
        synchronized(lock) {
            check(token);
            String descriptor=result.getString("descriptor","");
            if(!result.getBoolean("active")||descriptor.isEmpty())throw new IOException("Android did not register virtual DualSense");
            ownDescriptor=descriptor;getSharedPreferences("bridge",0).edit().putString("descriptor",descriptor).apply();
            if(physical(this).isEmpty()){endVirtual();throw new IOException("Controller disconnected during startup.");}
            active=true;message="Virtual DualSense verified. Offline access active; no Wi-Fi debugging connection.";
        }
    }
    private void selfTest(long token)throws Exception {
        boolean restore=active;
        try {
            HelperConnection.call(HelperConnection.OFF);active=false;waitForOwn(false,token);
            startVirtual(token);waitForOwn(true,token);
            HelperConnection.call(HelperConnection.OFF);active=false;waitForOwn(false,token);
            if(restore){startVirtual(token);waitForOwn(true,token);}
            message="PASS: Android registered and removed virtual DualSense using input API. "+SecurityState.describe(this);
        }catch(Exception e){endVirtual();throw e;}
    }
    private void waitForOwn(boolean wanted,long token)throws Exception {
        long end=SystemClock.elapsedRealtime()+4000;
        while(SystemClock.elapsedRealtime()<end){check(token);if(hasOwnDevice()==wanted)return;Thread.sleep(100);}
        throw new IOException(wanted?"Virtual controller missing":"Virtual controller still present after off");
    }
    private static String readDiagnostic(InputStream in)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1) {
            if(out.size()+n>2000000)throw new IOException("Unexpected diagnostic size");
            out.write(buffer,0,n);
            String text=new String(out.toByteArray(),StandardCharsets.UTF_8);
            int end=text.indexOf("CONTROLLER_BRIDGE_QUERY_DONE");
            if(end>=0)return text.substring(0,end);
        }
        throw new IOException("Android input check ended before completion. Try again.");
    }
    private static byte[] readAll(InputStream in,int limit)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Unexpected diagnostic size");out.write(buffer,0,n);}
        return out.toByteArray();
    }
    private void endVirtual(){
        final long token;
        synchronized(lock){active=false;token=generation;}
        if(HelperConnection.available()&&!worker.isShutdown())worker.execute(()->{
            // Serialize with ON. A newer request owns the final desired state.
            if(latestOnGeneration>token)return;
            try{HelperConnection.call(HelperConnection.OFF);}catch(Exception ignored){}
        });
    }
    @Override public void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);cancel("Virtual DualSense is off");final IBinder old=HelperConnection.detach();worker.shutdownNow();timers.shutdownNow();PhoneAdb owner=manager;if(owner!=null)new Thread(()->{try{owner.disconnect();}catch(IOException ignored){}},"adb-disconnect").start();if(old!=null)new Thread(()->{try{HelperConnection.call(old,HelperConnection.EXIT);}catch(Exception ignored){}},"helper-exit").start();super.onDestroy();}
}
