package app.controllerbridge;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Intent;
import android.os.*;

/** Temporary shell activation delegates only two input permissions to this app's UID. */
public final class ControllerInstrumentation extends Instrumentation {
    private UiAutomation automation;
    private String activationNonce;
    @Override public void onCreate(Bundle args){super.onCreate(args);activationNonce=args==null?null:args.getString("activation_nonce");start();}
    @Override public void onStart(){
        try {
            ActivationRequest.consume(getTargetContext(),activationNonce,()->{
                automation=getUiAutomation(3); // Preserve accessibility services; no accessibility client.
                automation.adoptShellPermissionIdentity("android.permission.INJECT_EVENTS",
                        "android.permission.ASSOCIATE_INPUT_DEVICE_TO_DISPLAY");
                HelperConnection.attachLocal(new ApiController(getTargetContext(),this::endAccess));
            });
            BridgeService.message="Input access ready. Turn both debugging modes OFF and restore Auto Blocker.";
            getTargetContext().startActivity(new Intent(getTargetContext(),MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            getTargetContext().startForegroundService(new Intent(getTargetContext(),BridgeService.class)
                    .setAction(BridgeService.CONNECT));
            Bundle ready=new Bundle();ready.putString("controller_bridge_ready","1");sendStatus(0,ready);
        }catch(Throwable e){
            android.util.Log.e("ControllerBridge","Input session activation failed",e);
            endAccess();
        }
    }
    private void endAccess(){
        HelperConnection.detach();
        try{if(automation!=null)automation.dropShellPermissionIdentity();}catch(Exception ignored){}
        try{finish(0,new Bundle());}catch(RuntimeException disconnected){}
        // The launcher Binder can already be dead. App death makes ActivityManager
        // finish instrumentation and revoke delegated permissions in that case too.
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
