package android.os;

/** 桌面端 Looper：仅作标识。 */
public class Looper {
    private static final Looper MAIN = new Looper();

    public static Looper getMainLooper() {
        return MAIN;
    }
}
