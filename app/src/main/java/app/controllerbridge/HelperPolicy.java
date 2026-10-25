package app.controllerbridge;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
/** Pure authorization checks shared by the bootstrap endpoint and helper. */
public final class HelperPolicy {
    private HelperPolicy() {}
    public static boolean owner(int caller, int expected) { return expected >= 10000 && caller == expected; }
    public static boolean bootstrap(int caller, String expected, String supplied, long now, long deadline) {
        return caller == 2000 && expected != null && supplied != null && expected.length() == 64
            && supplied.length() == 64 && now < deadline
            && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), supplied.getBytes(StandardCharsets.US_ASCII));
    }
}
