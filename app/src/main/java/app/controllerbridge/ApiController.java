package app.controllerbridge;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.*;
import android.view.InputDevice;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** Fixed Android 17 gamepad registration. No event injection or caller-supplied configuration. */
final class ApiController extends Binder {
    private static final String NAME="DualSense Wireless Controller";
    private final Context context;
    private final Runnable exit;
    private AutoCloseable pad;
    private String descriptor="";
    private boolean stopped;
    ApiController(Context context,Runnable exit){this.context=context;this.exit=exit;}
    @Override protected synchronized boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException {
        if(!HelperPolicy.owner(Binder.getCallingUid(),android.os.Process.myUid()))throw new SecurityException("Caller denied");
        data.enforceInterface(HelperConnection.INTERFACE);
        if(data.dataAvail()!=0||code<HelperConnection.STATUS||code>HelperConnection.EXIT)throw new SecurityException("Invalid controller operation");
        if(stopped)throw new IllegalStateException("Input access stopped");
        try {
            if(code==HelperConnection.ON)startController();
            if(code==HelperConnection.OFF)stopController();
            if(code==HelperConnection.EXIT){stopped=true;stopController();}
            Bundle result=new Bundle();result.putInt("uid",android.os.Process.myUid());
            result.putBoolean("active",pad!=null);result.putString("descriptor",descriptor);
            reply.writeNoException();reply.writeBundle(result);
            if(code==HelperConnection.EXIT)new Handler(Looper.getMainLooper()).postDelayed(exit,100);
            return true;
        }catch(Exception e){
            stopController();
            if(code==HelperConnection.EXIT)new Handler(Looper.getMainLooper()).post(exit);
            throw new IllegalStateException("Controller API failed: "+e,e);
        }
    }
    private void startController()throws Exception {
        if(pad!=null)return;
        Set<Integer> before=new HashSet<>();for(int id:InputDevice.getDeviceIds())before.add(id);
        Class<?> type=Class.forName("android.hardware.input.VirtualGamepadConfig");
        Object config=type.getConstructor().newInstance();
        type.getField("name").set(config,NAME);
        type.getField("vendorId").setInt(config,0x054c);
        type.getField("productId").setInt(config,0x0ce6);
        type.getField("associatedDisplayId").setInt(config,0);
        type.getField("registerTriggerAxes").setBoolean(config,true);
        pad=(AutoCloseable)InputManager.class.getMethod("createVirtualGamepad",type)
                .invoke(context.getSystemService(InputManager.class),config);
        long deadline=SystemClock.elapsedRealtime()+4000;
        while(SystemClock.elapsedRealtime()<deadline){
            for(int id:InputDevice.getDeviceIds()){
                InputDevice d=InputDevice.getDevice(id);
                if(!before.contains(id)&&d!=null&&NAME.equals(d.getName())&&d.getVendorId()==0x054c
                        &&d.getProductId()==0x0ce6&&(d.getSources()&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD){
                    descriptor=d.getDescriptor();return;
                }
            }
            Thread.sleep(100);
        }
        throw new IOException("Android did not register virtual DualSense");
    }
    private void stopController(){
        AutoCloseable old=pad;pad=null;descriptor="";
        if(old!=null)try{old.close();}catch(Exception e){
            // Never retain access after a failed close with unknown device state.
            stopped=true;new Handler(Looper.getMainLooper()).post(exit);
            android.util.Log.e("ControllerBridge","Failed to close virtual gamepad",e);
        }
    }
}
