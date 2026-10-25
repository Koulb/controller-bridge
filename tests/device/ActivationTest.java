package app.controllerbridge.tests;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import java.lang.reflect.*;

/** Separate signed test APK; never packaged into Controller Bridge. */
public final class ActivationTest extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void require(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    private static Context failWrites(Context target,boolean allowDelete){
        SharedPreferences real=target.getSharedPreferences("activation",0);
        final SharedPreferences.Editor[] editor=new SharedPreferences.Editor[1];
        editor[0]=(SharedPreferences.Editor)Proxy.newProxyInstance(ActivationTest.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(p,m,a)->{
            if(m.getName().equals("commit"))return false;
            if(m.getName().equals("apply"))return null;
            return editor[0];
        });
        SharedPreferences prefs=(SharedPreferences)Proxy.newProxyInstance(ActivationTest.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->m.getName().equals("edit")?editor[0]:m.invoke(real,a));
        return new ContextWrapper(target){
            @Override public SharedPreferences getSharedPreferences(String n,int mode){return prefs;}
            @Override public boolean deleteSharedPreferences(String n){return allowDelete&&target.deleteSharedPreferences(n);}
        };
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try {
            Context c=getTargetContext();
            Class<?> type=c.getClassLoader().loadClass("app.controllerbridge.ActivationRequest");
            Method arm=type.getDeclaredMethod("arm",Context.class),cancel=type.getDeclaredMethod("cancel",Context.class),consume=type.getDeclaredMethod("consume",Context.class,String.class,Runnable.class);
            arm.setAccessible(true);cancel.setAccessible(true);consume.setAccessible(true);
            require(c.checkSelfPermission("android.permission.INJECT_EVENTS")!=0,"Input permission remained after previous instrumentation ended");
            String token=(String)arm.invoke(null,c);
            require((Boolean)cancel.invoke(null,c),"Normal cancellation failed");
            final boolean[] granted={false};
            try{consume.invoke(null,c,token,(Runnable)()->granted[0]=true);throw new AssertionError("Cancelled request accepted");}
            catch(InvocationTargetException expected){require(expected.getCause() instanceof SecurityException,"Wrong rejection");}
            require(!granted[0],"Cancelled request reached permission adoption");
            arm.invoke(null,c);
            require(!(Boolean)cancel.invoke(null,failWrites(c,false)),"Revocation failure falsely reported success");
            require((Boolean)cancel.invoke(null,failWrites(c,true)),"Deletion fallback failed");
            require(!c.getSharedPreferences("activation",0).contains("token"),"Deletion left token accessible");
            token=(String)arm.invoke(null,c);
            consume.invoke(null,c,token,(Runnable)()->granted[0]=true);
            require(granted[0],"Valid request rejected");
            try{consume.invoke(null,c,token,(Runnable)()->{});throw new AssertionError("Token reused");}
            catch(InvocationTargetException expected){require(expected.getCause() instanceof SecurityException,"Wrong replay rejection");}
            result.putString("result","PASS: permission cleanup; cancelled request rejection; write failure reporting; deletion fallback; single-use consumption");
            finish(-1,result);
        }catch(Throwable error){result.putString("result","FAIL: "+error);android.util.Log.e("ControllerActivationTest","Failure",error);finish(0,result);}
    }
}
