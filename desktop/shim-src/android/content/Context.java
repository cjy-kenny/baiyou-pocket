package android.content;

import java.io.File;

/** 桌面端 Context：仅提供 Store/CrashGuard 需要的 getFilesDir()。
 *  数据目录默认 <工作目录>/desktop-data，可用 setDesktopFilesDir 覆盖（演示模式用独立目录）。 */
public class Context {
    private static File overrideDir;
    private final File filesDir;

    public Context() {
        filesDir = overrideDir != null
                ? overrideDir : new File("desktop-data").getAbsoluteFile();
    }

    public static void setDesktopFilesDir(File dir) {
        overrideDir = dir;
    }

    public File getFilesDir() {
        filesDir.mkdirs();
        return filesDir;
    }
}
