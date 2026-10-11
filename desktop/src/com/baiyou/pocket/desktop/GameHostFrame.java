package com.baiyou.pocket.desktop;

import android.content.Context;

import com.baiyou.pocket.CrashGuard;
import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;
import com.baiyou.pocket.data.ResultMerger;
import com.baiyou.pocket.data.Store;
import com.baiyou.pocket.game.Game;

import org.json.JSONObject;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** PC 端游戏宿主窗口：与 Android 端 GameHostActivity 同一套职责——
 *  HUD（分数/最高/暂停/重开/退出）、暂停遮罩、结算弹窗、会话数据回传合并。
 *  首局由本窗口在视图就绪后调用 game.restartGame() 启动（与 Android 端契约一致）。 */
public class GameHostFrame extends JFrame implements Game.Host {
    private final HomeFrame home;
    private final GameMeta meta;
    private Game game;
    private ViewHostPanel viewPanel;
    private JPanel pauseOverlay;
    private HostLayers layers;

    private JLabel tvScore, tvSub;
    private JButton btnPause;

    private boolean userPaused, settling, finishing;
    private boolean roundStarted, introShowing;
    private boolean clockOn = true;
    private long lastResumeAt, activeMs;
    private int accBest, accCoins, accPlays;
    private boolean roundAwarded, newBestThisRound;
    private int best;
    private JSONObject scoreObj;
    private boolean soundOn = true;

    public GameHostFrame(HomeFrame home, GameMeta meta) {
        super(meta.name + " · 百游口袋");
        this.home = home;
        this.meta = meta;

        soundOn = Store.loadStore(context()).optBoolean("sound", true);
        scoreObj = Store.loadScore(context(), meta.id);
        best = scoreObj.optInt("best", 0);
        lastResumeAt = System.currentTimeMillis();

        buildUi();

        game = Registry.create(meta.id);
        if (game == null) {
            toast("游戏加载失败");
            dispose();
            return;
        }
        game.bind(context(), this);
        viewPanel = new ViewHostPanel(game.createView());
        pauseOverlay = buildPauseOverlay();
        layers.addLayer(viewPanel);
        layers.addLayer(pauseOverlay);
        pauseOverlay.setVisible(false);
        showIntro(true); // 先看玩法介绍，点"开始"才开第一局

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitToHome();
            }
        });
    }

    private static Context context() {
        return new Context();
    }

    // ---------------- UI ----------------

    private void buildUi() {
        Theme t = Theme.get();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(t.bg);

        // 顶栏 HUD
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setBackground(t.card);
        JButton btnExit = barBtn("退出", t.textSub);
        btnExit.addActionListener(e -> exitToHome());

        JPanel nameCol = new JPanel(new GridLayout(2, 1, 0, 0));
        nameCol.setBackground(t.card);
        JLabel tvName = new JLabel(meta.name);
        tvName.setForeground(t.text);
        tvName.setFont(tvName.getFont().deriveFont(Font.BOLD, 15f));
        tvSub = new JLabel("最高 " + best);
        tvSub.setForeground(t.textSub);
        tvSub.setFont(tvSub.getFont().deriveFont(11f));
        nameCol.add(tvName);
        nameCol.add(tvSub);

        tvScore = new JLabel("0", SwingConstants.RIGHT);
        tvScore.setForeground(t.accent);
        tvScore.setFont(tvScore.getFont().deriveFont(Font.BOLD, 24f));
        tvScore.setPreferredSize(new Dimension(70, 30));

        btnPause = barBtn("暂停", t.text);
        btnPause.addActionListener(e -> userPause());
        JButton btnRestart = barBtn("重开", t.text);
        btnRestart.addActionListener(e -> userRestart());
        JButton btnHow = barBtn("玩法", t.textSub);
        btnHow.addActionListener(e -> showIntro(roundStarted));

        bar.add(btnExit);
        bar.add(nameCol);
        bar.add(tvScore);
        bar.add(btnPause);
        bar.add(btnRestart);
        bar.add(btnHow);

        // 游戏层 + 暂停遮罩（doLayout 时铺满）
        layers = new HostLayers();

        root.add(bar, BorderLayout.NORTH);
        root.add(layers, BorderLayout.CENTER);
        setContentPane(root);
        setSize(430, 780);
        setLocationRelativeTo(home);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    /** 分层容器：两个子层都铺满可用空间。 */
    private static class HostLayers extends JLayeredPane {
        void addLayer(java.awt.Component c) {
            add(c, DEFAULT_LAYER);
        }

        @Override
        public void doLayout() {
            for (java.awt.Component c : getComponents()) {
                c.setBounds(0, 0, getWidth(), getHeight());
            }
        }
    }

    private JButton barBtn(String s, Color color) {
        JButton b = new JButton(s);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        return b;
    }

    private JPanel buildPauseOverlay() {
        Theme t = Theme.get();
        JPanel ov = new JPanel(new BorderLayout());
        ov.setBackground(new Color(0xCC101218, true));

        JPanel btns = new JPanel(new GridLayout(3, 1, 0, 10));
        btns.setOpaque(false);
        btns.add(overlayBtn("继续游戏", t.accent, e -> userResume()));
        btns.add(overlayBtn("重新开始", Color.WHITE, e -> userRestart()));
        btns.add(overlayBtn("退出游戏", new Color(0x9AA0AC), e -> exitToHome()));

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(40, 70, 40, 70));
        JLabel title = new JLabel("已暂停", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JLabel hint = new JLabel("休息一下，回来继续", SwingConstants.CENTER);
        hint.setForeground(new Color(0x9AA0AC));
        center.add(title, BorderLayout.NORTH);
        center.add(hint, BorderLayout.CENTER);
        center.add(btns, BorderLayout.SOUTH);
        ov.add(center, BorderLayout.CENTER);
        return ov;
    }

    private JButton overlayBtn(String s, Color color, java.awt.event.ActionListener l) {
        JButton b = new JButton(s);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 18f));
        b.addActionListener(l);
        return b;
    }

    // ---------------- 暂停 / 重开 / 退出 ----------------

    private void userPause() {
        if (settling || finishing || userPaused) return;
        userPaused = true;
        syncClock(false);
        game.pauseGame();
        pauseOverlay.setVisible(true);
        pauseOverlay.requestFocus();
    }

    private void userResume() {
        userPaused = false;
        pauseOverlay.setVisible(false);
        syncClock(true);
        game.resumeGame();
    }

    private void userRestart() {
        if (settling || finishing) return;
        userPaused = false;
        pauseOverlay.setVisible(false);
        startRound();
    }

    private void startRound() {
        newBestThisRound = false;
        roundAwarded = false;
        roundStarted = true;
        tvScore.setText("0");
        syncClock(true);
        game.restartGame();
        viewPanel.repaint();
    }

    /** 玩法介绍弹窗。first=true 时按钮为"开始"，关闭后才开第一局。 */
    private void showIntro(boolean first) {
        introShowing = true;
        syncClock(false);
        if (game != null) game.pauseGame();
        pauseOverlay.setVisible(false);
        javax.swing.JDialog dlg = new javax.swing.JDialog(this, "玩法 · " + meta.name, true);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 10));
        Theme th = Theme.get();
        p.setBackground(th.card);
        p.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));
        JLabel lb = new JLabel("<html><div style='width:280px'>" + meta.tutorial + "</div></html>");
        lb.setForeground(th.text);
        lb.setFont(lb.getFont().deriveFont(Font.BOLD, 15f));
        p.add(lb);
        JButton go = new JButton(first ? "开始" : "继续");
        go.setForeground(Color.WHITE);
        go.setBackground(th.accent);
        go.setBorderPainted(false);
        go.setFocusPainted(false);
        go.addActionListener(e -> {
            dlg.dispose();
            introShowing = false;
            if (first) {
                startRound();
            } else {
                syncClock(true);
                game.resumeGame();
            }
        });
        p.add(go);
        dlg.setContentPane(p);
        dlg.setSize(380, 220);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    public void exitToHome() {
        if (finishing) return;
        settling = true;
        finishing = true;
        syncClock(false);
        if (!roundAwarded) {
            roundAwarded = true;
            accCoins += Math.min(50, Math.max(accBest, curScore()) / 10);
        }
        finishAndMerge();
        dispose();
        home.refresh();
        home.toFront();
    }

    private int curScore() {
        try {
            return Integer.parseInt(tvScore.getText());
        } catch (Exception e) {
            return 0;
        }
    }

    private void syncClock(boolean on) {
        long now = System.currentTimeMillis();
        if (on && !clockOn) {
            lastResumeAt = now;
            clockOn = true;
        } else if (!on && clockOn) {
            activeMs += now - lastResumeAt;
            clockOn = false;
        }
    }

    private void finishAndMerge() {
        ResultMerger.merge(context(), meta.id, accCoins, accPlays, accBest, (int) (activeMs / 1000));
    }

    // ---------------- Game.Host ----------------

    @Override
    public void setScore(int score) {
        tvScore.setText(String.valueOf(score));
        if (score > best) {
            best = score;
            newBestThisRound = true;
            tvSub.setText("最高 " + best);
            try {
                scoreObj.put("best", best);
                Store.saveScore(context(), meta.id, scoreObj);
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public void gameOver(int finalScore) {
        if (settling || finishing) return;
        settling = true;
        syncClock(false);
        game.pauseGame();

        if (finalScore > accBest) accBest = finalScore;
        if (!roundAwarded) {
            roundAwarded = true;
            accCoins += Math.min(50, Math.max(0, finalScore) / 10);
        }
        accPlays++;
        try {
            scoreObj.put("plays", scoreObj.optInt("plays", 0) + 1);
            scoreObj.put("last", System.currentTimeMillis());
            scoreObj.put("best", Math.max(best, finalScore));
            Store.saveScore(context(), meta.id, scoreObj);
        } catch (Throwable ignore) {
        }
        if (newBestThisRound && finalScore > 0) beep(3);

        String msg = "本局得分  " + finalScore
                + "<br>历史最高  " + Math.max(best, finalScore)
                + (newBestThisRound && finalScore > 0
                ? "<br><br>★ 新纪录！金币 +" + Math.min(50, finalScore / 10)
                : "<br><br>金币 +" + Math.min(50, Math.max(0, finalScore) / 10));

        Theme t = Theme.get();
        JDialog dlg = new JDialog(this, meta.name + " · 本局结束", ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 6));
        p.setBackground(t.card);
        p.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JLabel lb = new JLabel("<html>" + msg + "</html>");
        lb.setForeground(t.text);
        p.add(lb);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.setBackground(t.card);
        JButton again = new JButton("再来一局");
        JButton out = new JButton("退出");
        btns.add(again);
        btns.add(out);
        p.add(btns);
        dlg.setContentPane(p);
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dlg.setSize(320, 230);
        dlg.setLocationRelativeTo(this);
        again.addActionListener(e -> {
            dlg.dispose();
            settling = false;
            startRound();
        });
        out.addActionListener(e -> {
            dlg.dispose();
            exitToHome();
        });
        dlg.setVisible(true);
    }

    @Override
    public void toast(String msg) {
        home.toast(msg);
    }

    @Override
    public boolean soundOn() {
        return soundOn;
    }

    @Override
    public void beep(int kind) {
        if (soundOn) Beep.play(kind);
    }

    @Override
    public void crash(Throwable t) {
        if (finishing) return;
        finishing = true;
        CrashGuard.dump(context(), "game-" + meta.id, t);
        if (!roundAwarded) {
            roundAwarded = true;
            accCoins += Math.min(50, Math.max(accBest, curScore()) / 10);
        }
        finishAndMerge();
        JOptionPane.showMessageDialog(this,
                "游戏出现异常，已安全退出（其它内容不受影响）\n" + t,
                "提示", JOptionPane.WARNING_MESSAGE);
        dispose();
        home.refresh();
    }

    // ---------------- 窗口可见性即生命周期 ----------------

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        if (v) {
            if (introShowing) return; // 玩法弹窗期间计时暂停
            syncClock(true);
            if (!userPaused && !settling && game != null) game.resumeGame();
        } else {
            syncClock(false);
            if (game != null) {
                try {
                    game.pauseGame();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /** 演示模式访问器。 */
    public ViewHostPanel viewPanel() {
        return viewPanel;
    }
}
