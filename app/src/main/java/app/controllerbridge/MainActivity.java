package app.controllerbridge;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BACKGROUND=0xff0c121e, MUTED=0xff99a9bf, ACCENT=0xff65e3c4;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private TextView names,state,access,notice;
    private Button toggle,setupButton,resetButton;
    private ControllerVisual visual;
    private boolean foreground;
    private boolean virtualConnected;
    private String lastMessage="";
    private final Runnable update=new Runnable(){public void run(){render();handler.postDelayed(this,250);}};

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setPadding(0,dp(5),0,dp(5));return view;
    }
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(20));return d;}
    private Button button(String label){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(Color.WHITE);
        b.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33ffffff),shape(0xff243248),null));return b;
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(BACKGROUND);getWindow().setNavigationBarColor(BACKGROUND);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BACKGROUND);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(16),dp(24),dp(24));scroll.addView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("Controller Bridge",24,Color.WHITE);title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button menu=button("⋮");menu.setTextSize(25);menu.setContentDescription("More options");
        menu.setBackgroundColor(Color.TRANSPARENT);menu.setOnClickListener(this::menu);
        header.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(header);

        LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(18),dp(14),dp(18),dp(14));card.setBackground(shape(0xff182335));
        card.addView(text("CONNECTED CONTROLLER",11,MUTED));
        names=text("Looking for a controller…",19,Color.WHITE);names.setTypeface(null,Typeface.BOLD);card.addView(names);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(20),0,0);root.addView(card,cp);
        visual=new ControllerVisual(this);root.addView(visual,new LinearLayout.LayoutParams(-1,dp(220)));
        state=text("DualSense is off",25,Color.WHITE);state.setTypeface(null,Typeface.BOLD);state.setGravity(Gravity.CENTER);state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(state);
        access=text("",14,MUTED);access.setGravity(Gravity.CENTER);root.addView(access);
        toggle=button("Turn on DualSense");toggle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff147e6c));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(60));tp.setMargins(0,dp(22),0,dp(8));root.addView(toggle,tp);
        toggle.setOnClickListener(v->{
            if(virtualConnected||BridgeService.active||BridgeService.busy||BridgeService.pairing)send(BridgeService.OFF,0);
            else if(!HelperConnection.available())setup();
            else send(BridgeService.ON,0);
        });
        setupButton=button("Set up phone");setupButton.setOnClickListener(v->setup());root.addView(setupButton,new LinearLayout.LayoutParams(-1,dp(50)));
        notice=text("",13,MUTED);notice.setGravity(Gravity.CENTER);notice.setOnClickListener(v->securitySettings());root.addView(notice);
        resetButton=button("Reset setup");resetButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffa72e3b));
        resetButton.setOnClickListener(v->confirmReset());
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(50));rp.setMargins(0,dp(24),0,0);root.addView(resetButton,rp);
        setContentView(scroll);render();
    }
    private void render(){
        List<ControllerRules.Pad> pads=BridgeService.physical(this);boolean sony=false;
        StringBuilder list=new StringBuilder();
        for(ControllerRules.Pad pad:pads){if(list.length()>0)list.append("\n");list.append(pad.name);sony|=ControllerRules.isDualSense(pad);}
        boolean ready=HelperConnection.available(),active=registeredVirtual(),working=BridgeService.busy||BridgeService.pairing;
        virtualConnected=active;
        names.setText(pads.isEmpty()?"Connect a Bluetooth or USB gamepad":list.toString());
        state.setText(sony?"DualSense connected":active?"DualSense is on":"DualSense is off");
        state.setTextColor(active||sony?ACCENT:Color.WHITE);
        visual.setConnected(active||sony,foreground);
        toggle.setText(working?"Cancel":active?"Turn off DualSense":"Turn on DualSense");
        toggle.setVisibility(sony&&!active?View.GONE:View.VISIBLE);
        toggle.setEnabled(Build.VERSION.SDK_INT>=37&&(working||active||!pads.isEmpty()));
        toggle.setAlpha(toggle.isEnabled()?1f:.4f);
        setupButton.setVisibility(!ready&&!sony&&!working&&Build.VERSION.SDK_INT>=37?View.VISIBLE:View.GONE);
        boolean savedSetup=new java.io.File(getNoBackupFilesDir(),"adb-key.pk8").exists();
        resetButton.setVisibility(ready||savedSetup?View.VISIBLE:View.GONE);resetButton.setEnabled(!working);
        access.setText(Build.VERSION.SDK_INT<37?"Requires Android 17 or later":working?"Connecting…":sony?"Ready to play":active?"Fully close and reopen your game":ready?"Ready · Wi-Fi not needed":"Setup needed · Connect to Wi-Fi");
        String message=BridgeService.message;
        if(!Objects.equals(lastMessage,message)){
            lastMessage=message;
            if(message.startsWith("PASS:"))new AlertDialog.Builder(this).setTitle("Self-test passed").setMessage("Android created and removed the virtual DualSense.\n\n"+SecurityState.describe(this)).setPositiveButton("Done",null).show();
        }
        boolean usb=Settings.Global.getInt(getContentResolver(),"adb_enabled",0)!=0;
        boolean wireless=Settings.Global.getInt(getContentResolver(),"adb_wifi_enabled",0)!=0;
        if(working){notice.setText(message);notice.setClickable(false);}
        else if(isActionable(message)){notice.setText(message);notice.setClickable(false);}
        else if(ready){notice.setText(usb||wireless?"Turn debugging off, then restore Auto Blocker.\nOpen security settings":"");notice.setClickable(usb||wireless);}
        else {notice.setText(sony?"":"Wi-Fi activation is needed again after reboot or app termination.");notice.setClickable(false);}
        notice.setVisibility(notice.length()==0?View.GONE:View.VISIBLE);
    }
    private boolean registeredVirtual(){
        String descriptor=BridgeService.ownDescriptor;
        if(descriptor.isEmpty())descriptor=getSharedPreferences("bridge",0).getString("descriptor","");
        if(descriptor.isEmpty())return false;
        for(int id:android.view.InputDevice.getDeviceIds()){
            android.view.InputDevice device=android.view.InputDevice.getDevice(id);
            if(device!=null&&descriptor.equals(device.getDescriptor())&&device.getVendorId()==0x054c&&device.getProductId()==0x0ce6)return true;
        }
        return false;
    }
    private boolean isActionable(String message){
        return !message.isEmpty()&&!message.startsWith("PASS:")&&!message.equals("Set up phone access to begin")
                &&!message.startsWith("Input access ready")&&!message.startsWith("Offline phone access ready")
                &&!message.startsWith("Virtual DualSense verified")&&!message.equals("Virtual DualSense is off")
                &&!message.startsWith("Restarting app")&&!message.startsWith("Virtual controller verified");
    }
    private void menu(View anchor){
        PopupMenu popup=new PopupMenu(this,anchor);
        popup.getMenu().add("Set up phone").setOnMenuItemClickListener(i->{setup();return true;});
        popup.getMenu().add("Run self-test").setOnMenuItemClickListener(i->{if(HelperConnection.available())send(BridgeService.TEST,0);else setup();return true;});
        popup.getMenu().add("Security settings").setOnMenuItemClickListener(i->{securitySettings();return true;});
        popup.getMenu().add("Stop phone access").setOnMenuItemClickListener(i->{
            new AlertDialog.Builder(this).setTitle("Stop phone access?").setMessage("Closes the app and revokes input access. Wi-Fi activation will be needed again.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Stop",(d,w)->send(BridgeService.EXIT,0)).show();return true;
        });
        popup.getMenu().add("Open-source licenses").setOnMenuItemClickListener(i->{licenses();return true;});popup.show();
    }
    private boolean send(String action,int port){
        Intent intent=new Intent(this,BridgeService.class).setAction(action);if(port>0)intent.putExtra("port",port);
        return send(intent);
    }
    private boolean send(Intent intent){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},1);Toast.makeText(this,"Allow notifications, then tap again.",Toast.LENGTH_LONG).show();return false;
        }
        startForegroundService(intent);return true;
    }
    private void wirelessSettings(){
        try{startActivity(new Intent("android.settings.WIRELESS_DEBUGGING_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(Exception e){
            Toast.makeText(this,"Open Wireless debugging, then Pair device with pairing code.",Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
    private void securitySettings(){
        try{startActivity(new Intent("com.samsung.android.rampart.action.MAIN_SETTING_ACTIVITY"));}
        catch(Exception e){startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));}
    }
    private void setup(){
        if(Build.VERSION.SDK_INT<37){new AlertDialog.Builder(this).setMessage("This version requires Android 17 or later.").setPositiveButton("Done",null).show();return;}
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(24),dp(8),dp(24),dp(8));
        panel.addView(text("1. Connect to trusted Wi-Fi. Temporarily turn Auto Blocker off.\n\n2. Enable Developer options → Wireless debugging.\n\n3. Tap Pair phone. In Android, choose Pair device with pairing code. Keep Android’s code dialog open. Pull down notifications, expand Controller Bridge, tap Enter pairing code, type the six digits and tap Send.",15,0xffd7e1ef));
        Button settings=button("Wireless debugging settings");settings.setOnClickListener(v->wirelessSettings());panel.addView(settings);
        Button pair=button("Pair phone");pair.setOnClickListener(v->{if(send(BridgeService.PAIR,0))wirelessSettings();});panel.addView(pair);
        panel.addView(text("Already paired? Enable Wireless debugging, then activate.",14,MUTED));
        Button activate=button("Activate paired phone");activate.setOnClickListener(v->send(BridgeService.CONNECT,0));panel.addView(activate);
        panel.addView(text("The app restarts during activation. Afterward, turn both debugging modes off and restore Auto Blocker. Wi-Fi is then optional.",14,MUTED));
        Button protection=button("Auto Blocker settings");protection.setOnClickListener(v->securitySettings());panel.addView(protection);
        Button fallback=button("Connection troubleshooting");fallback.setOnClickListener(v->manualConnection());panel.addView(fallback);
        ScrollView scroll=new ScrollView(this);scroll.addView(panel);
        new AlertDialog.Builder(this).setTitle("Set up phone").setView(scroll).setPositiveButton("Done",null).show();
    }
    private void confirmReset(){
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Reset setup?")
                .setMessage("Are you sure? This removes the virtual controller, revokes phone access, deletes this app’s pairing key and settings, and closes the app. You will need to pair again.")
                .setNegativeButton("Cancel",null).setPositiveButton("Reset",(d,w)->{
                    try{
                        if(!getSystemService(ActivityManager.class).clearApplicationUserData())throw new IllegalStateException();
                    }catch(RuntimeException e){Toast.makeText(this,"Reset failed. Try again or clear app data in Android Settings.",Toast.LENGTH_LONG).show();}
                }).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xffff818c));dialog.show();
    }
    private void manualConnection(){
        EditText port=new EditText(this);port.setInputType(InputType.TYPE_CLASS_NUMBER);port.setHint("Connection port");port.setPadding(dp(24),dp(12),dp(24),dp(12));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Connect using port")
                .setMessage("Use the port on Android’s main Wireless debugging screen, not the pairing port.").setView(port)
                .setNegativeButton("Cancel",null).setPositiveButton("Connect",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{int value=Integer.parseInt(port.getText().toString());if(value<1||value>65535)throw new NumberFormatException();if(send(BridgeService.CONNECT,value))dialog.dismiss();}
            catch(NumberFormatException e){port.setError("Enter a port from 1 to 65535");}
        }));dialog.show();
    }
    private void licenses(){
        try{
            String[] files=getAssets().list("licenses");String[] labels=new String[files.length+1];labels[0]="Third-party notices";System.arraycopy(files,0,labels,1,files.length);
            new AlertDialog.Builder(this).setTitle("Open-source licenses").setItems(labels,(dialog,index)->{
                try(java.io.InputStream in=getAssets().open(index==0?"THIRD_PARTY_NOTICES.txt":"licenses/"+files[index-1])){
                    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                    TextView content=text(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8),13,0xffd7e1ef);content.setTextIsSelectable(true);content.setPadding(dp(20),dp(12),dp(20),dp(12));
                    ScrollView view=new ScrollView(this);view.addView(content);new AlertDialog.Builder(this).setTitle(labels[index]).setView(view).setPositiveButton("Close",null).show();
                }catch(Exception e){Toast.makeText(this,"License text unavailable",Toast.LENGTH_LONG).show();}
            }).setNegativeButton("Close",null).show();
        }catch(Exception e){Toast.makeText(this,"License text unavailable",Toast.LENGTH_LONG).show();}
    }
    @Override public void onResume(){super.onResume();foreground=true;handler.post(update);}
    @Override public void onPause(){foreground=false;handler.removeCallbacks(update);visual.stopAnimation();super.onPause();}
}
