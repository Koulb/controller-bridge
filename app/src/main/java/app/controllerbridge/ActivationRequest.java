package app.controllerbridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.io.IOException;
import java.security.SecureRandom;

/** A cancelled request cannot be resurrected by a detached launcher after app restart. */
final class ActivationRequest {
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("activation",0);}
    static synchronized String arm(Context c)throws IOException {
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);
        StringBuilder token=new StringBuilder();for(byte b:random)token.append(String.format("%02x",b&255));
        if(!prefs(c).edit().putString("token",token.toString()).putLong("deadline",SystemClock.elapsedRealtime()+60000).commit())throw new IOException("Could not save activation request");
        return token.toString();
    }
    static synchronized boolean cancel(Context c){
        SharedPreferences p=prefs(c);
        if(p.edit().clear().commit())return true;
        // Deletion can still work when a replacement preference file cannot be written.
        // If both fail, report failure rather than crash or claim access was revoked.
        return c.deleteSharedPreferences("activation");
    }
    static synchronized void consume(Context c,String supplied,Runnable activate){
        SharedPreferences p=prefs(c);
        // Android itself restricts instrumentation startup to shell/system callers.
        // Reuse the tested constant-time token/expiry policy for the app's request.
        if(!HelperPolicy.bootstrap(2000,p.getString("token",null),supplied,SystemClock.elapsedRealtime(),p.getLong("deadline",0)))throw new SecurityException("Activation cancelled or expired");
        if(!p.edit().clear().commit())throw new IllegalStateException("Could not consume activation request");
        activate.run();
    }
}
