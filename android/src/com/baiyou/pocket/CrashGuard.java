package com.baiyou.pocket;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 崩溃守卫：任何进程内出现未捕获异常时，先把堆栈写入 files/logs/crash-*.log，
 *  再交回系统默认处理。
 *
 *  与 Manifest 中 GameHostActivity 的 android:process=":game" 配合形成双保险：
 *  - 可预期异常（逻辑输入等）由宿主 SafeFrame / 游戏循环捕获，游戏内消化；
 *  - 不可预期异常（OOM、native 崩溃等）最多杀死 :game 进程，主界面与其它游戏不受影响。 */
public final class CrashGuard implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler prev;
    private final File dir;

    public static void install(android.content.Context ctx) {
        Thread.setDefaultUncaughtExceptionHandler(
                new CrashGuard(Thread.getDefaultUncaughtExceptionHandler(),
                        new File(ctx.getFilesDir(), "logs")));
    }

    /** 主动记录一条异常日志（游戏内被捕获的异常也留档，便于后续修复）。 */
    public static void dump(android.content.Context ctx, String tag, Throwable e) {
        try {
            File dir = new File(ctx.getFilesDir(), "logs");
            dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            Writer w = new OutputStreamWriter(
                    new FileOutputStream(new File(dir, tag + "-" + ts + ".log")), "UTF-8");
            PrintWriter pw = new PrintWriter(w);
            pw.println("tag=" + tag);
            e.printStackTrace(pw);
            pw.close();
        } catch (Throwable ignore) {
        }
    }

    private CrashGuard(Thread.UncaughtExceptionHandler prev, File dir) {
        this.prev = prev;
        this.dir = dir;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            Writer w = new OutputStreamWriter(
                    new FileOutputStream(new File(dir, "crash-" + ts + ".log")), "UTF-8");
            PrintWriter pw = new PrintWriter(w);
            pw.println("thread=" + t.getName());
            e.printStackTrace(pw);
            pw.close();
        } catch (Throwable ignore) {
        }
        if (prev != null) prev.uncaughtException(t, e);
    }
}
