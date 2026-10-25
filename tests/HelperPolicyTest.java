package app.controllerbridge;

public final class HelperPolicyTest {
    private static int checks;
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Authorization check " + checks);
    }
    public static void main(String[] args) {
        String token = new String(new char[64]).replace('\0', 'a');
        String other = new String(new char[64]).replace('\0', 'b');
        check(HelperPolicy.owner(10234, 10234));
        check(!HelperPolicy.owner(10235, 10234));
        check(!HelperPolicy.owner(2000, 10234));
        check(!HelperPolicy.owner(0, 10234));
        check(!HelperPolicy.owner(2000, 2000));
        check(HelperPolicy.bootstrap(2000, token, token, 99, 100));
        check(!HelperPolicy.bootstrap(10234, token, token, 99, 100));
        check(!HelperPolicy.bootstrap(0, token, token, 99, 100));
        check(!HelperPolicy.bootstrap(2000, token, other, 99, 100));
        check(!HelperPolicy.bootstrap(2000, token, token, 100, 100));
        check(!HelperPolicy.bootstrap(2000, token, token, 101, 100));
        check(!HelperPolicy.bootstrap(2000, null, token, 99, 100));
        check(!HelperPolicy.bootstrap(2000, token, null, 99, 100));
        check(!HelperPolicy.bootstrap(2000, token, "short", 99, 100));
        System.out.println("HelperPolicyTest: " + checks + " checks passed");
    }
}
