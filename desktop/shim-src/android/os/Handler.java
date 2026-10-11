package android.os;

import java.util.HashMap;
import java.util.Map;

/** Handler 垫片：可插拔驱动。
 *  - 默认 SwingTimerDriver：回调跑在 Swing EDT（PC 桌面端，与 Android 单线程模型一致）；
 *  - 网页端（WebServer）启动前调用 setDriver 切到单线程调度器，
 *    让游戏逻辑/渲染/输入全部串行在一条"游戏线程"上，免锁。 */
public class Handler {
    public interface Driver {
        void post(Runnable r, long delayMs);

        void cancel(Runnable r);
    }

    /** 默认驱动：javax.swing.Timer（EDT）。 */
    private static class SwingTimerDriver implements Driver {
        private final Map<Runnable, javax.swing.Timer> timers = new HashMap<>();

        @Override
        public void post(final Runnable r, long delayMs) {
            cancel(r);
            javax.swing.Timer t = new javax.swing.Timer((int) Math.max(0, delayMs), e -> {
                synchronized (timers) {
                    timers.remove(r);
                }
                r.run();
            });
            t.setRepeats(false);
            t.setCoalesce(false);
            synchronized (timers) {
                timers.put(r, t);
            }
            t.start();
        }

        @Override
        public void cancel(Runnable r) {
            javax.swing.Timer t;
            synchronized (timers) {
                t = timers.remove(r);
            }
            if (t != null) t.stop();
        }
    }

    private static Driver driver = new SwingTimerDriver();

    /** 网页端在创建任何游戏之前调用。 */
    public static void setDriver(Driver d) {
        if (d != null) driver = d;
    }

    public Handler(Looper looper) {
    }

    public boolean postDelayed(final Runnable r, long delayMs) {
        driver.post(r, Math.max(0, delayMs));
        return true;
    }

    public void removeCallbacks(Runnable r) {
        driver.cancel(r);
    }
}
