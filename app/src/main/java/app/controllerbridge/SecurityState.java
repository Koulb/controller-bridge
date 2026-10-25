package app.controllerbridge;
import android.content.Context;
import android.provider.Settings;
import android.net.wifi.WifiManager;
public final class SecurityState {
    private SecurityState() {}
    public static String activationAdvice(Context c) {
        return "OFF".equals(flag(c,"adb_wifi_enabled",false))
            ? "Wireless debugging is OFF. Restore Auto Blocker; Wi-Fi is no longer needed."
            : "Turn Wireless debugging OFF in Settings, then restore Auto Blocker. Helper access remains available.";
    }
    private static String flag(Context c,String key,boolean secure) {
        try {int value=secure?Settings.Secure.getInt(c.getContentResolver(),key):Settings.Global.getInt(c.getContentResolver(),key);return value==1?"ON":value==0?"OFF":"unknown";}
        catch(Exception e){return "check Settings";}
    }
    public static String describe(Context c) {
        String wifi="unknown";try{wifi=((WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled()?"ON":"OFF";}catch(Exception ignored){}
        return "Wi-Fi: "+wifi+" · Wireless debugging: "+flag(c,"adb_wifi_enabled",false)+" · USB debugging: "+flag(c,"adb_enabled",false)+" · Auto Blocker: "+flag(c,"rampart_main_switch_enabled",true);
    }
}
