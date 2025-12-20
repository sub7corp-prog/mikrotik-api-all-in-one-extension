package android.util;

public final class Log {
    private Log() {}

    public static int d(String tag, String msg) {
        System.out.println("[D/" + tag + "] " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        System.err.println("[E/" + tag + "] " + msg);
        return 0;
    }
}
