package com.baiyou.pocket.desktop;

import android.content.Context;

import com.baiyou.pocket.CrashGuard;

import javax.swing.SwingUtilities;
import java.io.File;

/** PC 端入口。
 *  无参数：打开首页窗口正常游玩；
 *  --web [端口]：网页壳（默认 8080），浏览器实时画面，进度与 PC 端共用存档；
 *  --demo：演示模式，用独立数据目录（desktop-data-demo）；
 *  --demo --web 8082：演示目录 + 网页服务（自动化自测用，互不污染真档）；
 *  --demo（单独）：自动打开 5 款原型、注入触摸操作并截图。 */
public class DesktopMain {
    public static void main(String[] args) {
        boolean web = false, demo = false;
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--web")) {
                web = true;
                if (i + 1 < args.length && args[i + 1].matches("\\d+")) {
                    port = Integer.parseInt(args[i + 1]);
                }
            } else if (args[i].equals("--demo")) {
                demo = true;
            }
        }
        Context.setDesktopFilesDir(new File(demo ? "desktop-data-demo" : "desktop-data").getAbsoluteFile());
        CrashGuard.install(new Context());
        if (web) {
            WebServer.start(port);
            return; // 服务线程为非守护，主线程返回后进程常驻
        }
        final boolean demoFinal = demo;
        SwingUtilities.invokeLater(() -> {
            HomeFrame home = new HomeFrame();
            home.setVisible(true);
            if (demoFinal) DemoRunner.run(home);
        });
    }
}
