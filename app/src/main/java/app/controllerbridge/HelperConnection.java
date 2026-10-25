package app.controllerbridge;

import android.os.*;
import java.io.IOException;

/** Process-local controller capability; never exported or registered with a service manager. */
public final class HelperConnection {
    public static final String INTERFACE="app.controllerbridge.ControllerOnly.v2";
    public static final int STATUS=1, ON=2, OFF=3, EXIT=4;
    private static volatile IBinder local;
    static synchronized void attachLocal(IBinder session){
        if(local!=null)throw new IllegalStateException("Input session already active");
        local=session;
    }
    public static boolean available(){return local!=null;}
    static synchronized IBinder detach(){IBinder old=local;local=null;return old;}
    public static Bundle call(int command)throws Exception{return call(local,command);}
    static Bundle call(IBinder endpoint,int command)throws Exception {
        if(endpoint==null)throw new IOException("Activate phone access after restart.");
        Parcel input=Parcel.obtain(),output=Parcel.obtain();
        try {
            input.writeInterfaceToken(INTERFACE);
            if(!endpoint.transact(command,input,output,0))throw new IOException("Unsupported controller operation");
            output.readException();return output.readBundle(HelperConnection.class.getClassLoader());
        }finally{input.recycle();output.recycle();}
    }
}
