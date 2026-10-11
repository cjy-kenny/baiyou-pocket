package com.baiyou.pocket.desktop;

import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Map;

/** 演示模式：真实启动每一款原型游戏 → 按脚本注入触摸 → 截图保存。
 *  截图经 frame.paint 在 EDT 上同步渲染，无需截屏权限，窗口被遮挡也能出图。 */
public final class DemoRunner {
    private DemoRunner() {
    }

    /** 每款游戏的操作脚本：{action, fx, fy, 完成后等待ms}（面板比例坐标）。
     *  action：0按下 1抬起 2拖动 */
    private static final Map<String, float[][]> SCRIPTS = new HashMap<>();

    static {
        SCRIPTS.put("g001", new float[][]{           // 方块消消：点几组同色
                {0, .30f, .38f, 80}, {1, .30f, .38f, 380},
                {0, .58f, .45f, 80}, {1, .58f, .45f, 380},
                {0, .40f, .58f, 80}, {1, .40f, .58f, 700}});
        SCRIPTS.put("g002", new float[][]{           // 贪吃蛇：尽快向上再向左
                {0, .50f, .68f, 60}, {2, .50f, .55f, 60}, {1, .50f, .45f, 400},
                {0, .50f, .45f, 60}, {2, .38f, .45f, 60}, {1, .30f, .45f, 500},
                {0, .40f, .55f, 60}, {2, .40f, .60f, 60}, {1, .40f, .62f, 600}});
        SCRIPTS.put("g003", new float[][]{           // 记忆翻牌：翻四张（末步零延迟，截图恰在翻开窗口内）
                {0, .28f, .28f, 80}, {1, .28f, .28f, 800},
                {0, .55f, .28f, 80}, {1, .55f, .28f, 800},
                {0, .28f, .42f, 80}, {1, .28f, .42f, 800},
                {0, .55f, .42f, 80}, {1, .55f, .42f, 0}});
        SCRIPTS.put("g004", new float[][]{           // 弹球：发射并拖动挡板
                {0, .50f, .90f, 120}, {2, .62f, .90f, 300},
                {2, .40f, .90f, 300}, {2, .55f, .90f, 600}, {1, .55f, .90f, 400}});
        SCRIPTS.put("g005", new float[][]{           // 数字合并：上下左滑三次
                {0, .50f, .62f, 80}, {1, .50f, .26f, 420},
                {0, .35f, .40f, 80}, {1, .68f, .40f, 420},
                {0, .50f, .62f, 80}, {1, .50f, .26f, 700}});
    }

    public static void run(HomeFrame home) {
        Thread th = new Thread(() -> {
            try {
                File outDir = new File("desktop", "screenshots");
                outDir.mkdirs();

                Thread.sleep(1500);
                shot(home, new File(outDir, "home.png"));

                // 100 款全跑要十分钟：抽样开放类的代表作 + 全部原型
                String[] sample = {"g001", "g005", "g002", "g003", "g004",
                        "g024", "g042", "g061", "g071", "g081", "g091", "g100"};
                List<GameMeta> list = new ArrayList<>();
                for (String id : sample) {
                    GameMeta m = Registry.get(id);
                    if (m != null && m.playable()) list.add(m);
                }
                for (GameMeta m : list) {
                    final GameHostFrame[] holder = new GameHostFrame[1];
                    SwingUtilities.invokeAndWait(() -> holder[0] = home.openGame(m));
                    Thread.sleep(700);
                    float[][] script = SCRIPTS.get(m.id);
                    if (script != null) {
                        for (float[] s : script) {
                            SwingUtilities.invokeLater(() -> {
                                if (holder[0].viewPanel() != null) {
                                    holder[0].viewPanel().inject((int) s[0], s[1], s[2]);
                                }
                            });
                            Thread.sleep((int) s[3]);
                        }
                    }
                    Thread.sleep(700);
                    shot(holder[0], new File(outDir, "game-" + m.id + ".png"));
                    SwingUtilities.invokeAndWait(holder[0]::exitToHome);
                    Thread.sleep(500);
                }
                System.out.println("demo done -> " + outDir.getAbsolutePath());
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }, "demo-runner");
        th.setDaemon(true);
        th.start();
    }

    private static void shot(final java.awt.Window w, final File out) throws Exception {
        Runnable r = () -> {
            try {
                BufferedImage img = new BufferedImage(w.getWidth(), w.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                w.paint(img.getGraphics());
                ImageIO.write(img, "png", out);
                System.out.println("shot: " + out.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeAndWait(r);
    }
}
