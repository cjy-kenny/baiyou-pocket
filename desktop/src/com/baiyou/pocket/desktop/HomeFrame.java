package com.baiyou.pocket.desktop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.baiyou.pocket.data.Achievements;
import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;
import com.baiyou.pocket.data.Store;
import com.baiyou.pocket.ui.IconDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** PC 端首页：与 Android 端 MainActivity 同一套数据与交互——
 *  100 款入口、分类 chips、搜索、收藏（右键）、最近玩过、成就、设置、深/浅主题。
 *  卡片为自绘面板（与手机端观感一致），图标直接复用 IconDrawable。 */
public class HomeFrame extends JFrame {
    private final Map<String, BufferedImage> iconCache = new HashMap<>();
    private final Map<String, JSONObject> scoreCache = new HashMap<>();
    private JPanel header, chipsRow;
    private JTextField search;
    private BoardPanel board;
    private JLabel tvCoins, tvSubTitle;
    private String cat = "全部";
    private String query = "";
    private JSONObject store;

    public HomeFrame() {
        super("百游口袋 · PC 端");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1040, 720);
        setLocationRelativeTo(null);
        buildUi();
        refresh();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private Context ctx() {
        return new Context();
    }

    // ---------------- UI ----------------

    private void buildUi() {
        Theme t = Theme.get();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(t.bg);

        // ---- 头部 ----
        header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(t.bg);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new javax.swing.BoxLayout(titleCol, javax.swing.BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);
        JLabel tvTitle = new JLabel("百游口袋");
        tvTitle.setForeground(t.text);
        tvTitle.setFont(tvTitle.getFont().deriveFont(Font.BOLD, 24f));
        tvSubTitle = new JLabel();
        tvSubTitle.setForeground(t.textSub);
        tvSubTitle.setFont(tvSubTitle.getFont().deriveFont(12f));
        titleCol.add(tvTitle);
        titleCol.add(tvSubTitle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        tvCoins = pill("", t.accent);
        tvCoins.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showAchievements();
            }
        });
        right.add(tvCoins);
        right.add(headerBtn("成就", e -> showAchievements()));
        right.add(headerBtn("设置", e -> showSettings()));
        right.add(headerBtn("主题", e -> {
            Theme.get().dark = !Theme.get().dark;
            Theme.get().apply();
            iconCache.clear();
            buildUi();
            refresh();
        }));

        header.add(titleCol, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);

        // ---- 搜索 ----
        search = new JTextField();
        search.setToolTipText("按名称或分类搜索");
        search.setOpaque(true);
        search.setForeground(t.text);
        search.setCaretColor(t.text);
        search.setBackground(t.card);
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.cardSub, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            private void up() {
                query = search.getText().trim();
                board.refresh();
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                up();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                up();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                up();
            }
        };
        search.getDocument().addDocumentListener(dl);

        // ---- chips ----
        chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        chipsRow.setBackground(t.bg);
        chipsRow.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 12));

        // ---- 内容 ----
        board = new BoardPanel();
        JScrollPane scroll = new JScrollPane(board,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        root.add(scroll, BorderLayout.CENTER);
        // 顶部三行：header / 搜索 / 分类 chips
        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setBackground(t.bg);
        searchWrap.setBorder(BorderFactory.createEmptyBorder(6, 16, 4, 16));
        searchWrap.add(search, BorderLayout.CENTER);
        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(t.bg);
        north.add(header, BorderLayout.NORTH);
        north.add(searchWrap, BorderLayout.CENTER);
        north.add(chipsRow, BorderLayout.SOUTH);
        root.add(north, BorderLayout.NORTH);

        setContentPane(root);
    }

    private JLabel pill(String s, Color color) {
        JLabel l = new JLabel(s);
        l.setForeground(color);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 13f));
        l.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    private JButton headerBtn(String s, java.awt.event.ActionListener l) {
        JButton b = new JButton(s);
        b.setForeground(Theme.get().text);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(l);
        return b;
    }

    private void rebuildChips() {
        Theme t = Theme.get();
        chipsRow.removeAll();
        List<String> labels = new ArrayList<>();
        labels.add("全部");
        labels.add("收藏");
        for (String c : Registry.categories()) labels.add(c);
        for (String label : labels) {
            boolean on = label.equals(cat);
            JButton b = new JButton(label);
            b.setForeground(on ? Color.WHITE : t.textSub);
            b.setBackground(on ? t.accent : t.card);
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setFont(b.getFont().deriveFont(on ? Font.BOLD : Font.PLAIN, 13f));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cat = label;
                    rebuildChips();
                    board.refresh();
                }
            });
            chipsRow.add(b);
        }
        chipsRow.revalidate();
        chipsRow.repaint();
    }

    // ---------------- 数据 ----------------

    public void refresh() {
        store = Store.loadStore(ctx());
        scoreCache.clear();
        for (GameMeta m : Registry.ALL) scoreCache.put(m.id, Store.loadScore(ctx(), m.id));
        rebuildChips();
        JSONArray recent = store.optJSONArray("recent");
        tvCoins.setText("金币 " + store.optInt("coins", 0));
        tvSubTitle.setText("100 款全开放 · 累计 " + store.optInt("playsTotal", 0) + " 局"
                + (recent != null && recent.length() > 0 ? " · 最近玩了 " + recent.length() + " 款" : ""));
        if (board != null) board.refresh();
    }

    private int playableCount() {
        int n = 0;
        for (GameMeta m : Registry.ALL) if (m.playable()) n++;
        return n;
    }

    private boolean isFav(String id) {
        JSONArray favs = store.optJSONArray("favs");
        for (int i = 0; i < favs.length(); i++) if (favs.optString(i).equals(id)) return true;
        return false;
    }

    private void toggleFav(GameMeta m) {
        JSONArray favs = store.optJSONArray("favs");
        boolean has = false;
        for (int i = 0; i < favs.length(); i++) if (favs.optString(i).equals(m.id)) has = true;
        if (has) {
            for (int i = favs.length() - 1; i >= 0; i--) if (favs.optString(i).equals(m.id)) favs.remove(i);
            toast("已取消收藏《" + m.name + "》");
        } else {
            favs.put(m.id);
            toast("已收藏《" + m.name + "》");
        }
        Store.saveStore(ctx(), store);
        refresh();
    }

    public GameHostFrame openGame(GameMeta m) {
        GameHostFrame f = new GameHostFrame(this, m);
        f.setVisible(true);
        return f;
    }

    // ---------------- 自绘内容面板 ----------------

    private class BoardPanel extends JPanel {
        private final List<Object[]> hits = new ArrayList<>(); // {Rectangle, GameMeta}
        private final int PAD = 16;

        BoardPanel() {
            setOpaque(true);
            setBackground(Theme.get().bg);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GameMeta m = hit(e.getX(), e.getY());
                    if (m == null) return;
                    if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
                        toggleFav(m);
                        return;
                    }
                    if (m.playable()) openGame(m);
                    else toast("《" + m.name + "》正在开发中，敬请期待");
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(hit(e.getX(), e.getY()) != null
                            ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                }
            });
        }

        GameMeta hit(int x, int y) {
            for (Object[] h : hits) {
                Rectangle r = (Rectangle) h[0];
                if (r.contains(x, y)) return (GameMeta) h[1];
            }
            return null;
        }

        void refresh() {
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int w = Math.max(getParent() == null ? 900 : getParent().getWidth(), 700);
            return new Dimension(w, estimateHeight(w));
        }

        private int estimateHeight(int w) {
            int cols = cols(w);
            List<GameMeta> list = filtered();
            boolean browse = cat.equals("全部") && query.isEmpty();
            int h = PAD + 26;
            if (browse && recents().length() > 0) h += 26 + 118;
            h += 30 + ((list.size() + cols - 1) / cols) * (130 + 10) + 50;
            return h + 20;
        }

        private int cols(int w) {
            return Math.max(2, Math.min(5, (w - PAD * 2) / 165));
        }

        private List<GameMeta> filtered() {
            List<GameMeta> list = new ArrayList<>();
            boolean favOnly = cat.equals("收藏");
            boolean browse = cat.equals("全部") && query.isEmpty();
            for (GameMeta m : Registry.ALL) {
                boolean ok;
                if (favOnly) ok = isFav(m.id);
                else if (browse) ok = true;
                else ok = m.category.equals(cat);
                if (ok && !query.isEmpty()) ok = m.name.contains(query) || m.category.contains(query);
                if (ok) list.add(m);
            }
            return list;
        }

        private JSONArray recents() {
            return store == null ? new JSONArray() : store.optJSONArray("recent");
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Theme t = Theme.get();
            hits.clear();
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int y = PAD;

            boolean browse = cat.equals("全部") && query.isEmpty();
            JSONArray recent = recents();
            if (browse && recent.length() > 0) {
                y = drawSectionLabel(g, "最近玩过", y, t);
                int x = PAD;
                int shown = 0;
                for (int i = 0; i < recent.length() && shown < 10; i++) {
                    GameMeta m = Registry.get(recent.optJSONObject(i).optString("id"));
                    if (m == null) continue;
                    drawMiniCard(g, m, x, y, t);
                    hits.add(new Object[]{new Rectangle(x, y, 96, 112), m});
                    x += 104;
                    shown++;
                }
                y += 118 + 8;
            }

            List<GameMeta> list = filtered();
            String title = (cat.equals("收藏") ? "收藏" : (browse ? "全部游戏" : cat))
                    + " · " + list.size() + " 款";
            y = drawSectionLabel(g, title, y, t);

            int cols = cols(w);
            int cardW = (w - PAD * 2 - (cols - 1) * 10) / cols;
            int cardH = 122;
            int x = PAD;
            for (int i = 0; i < list.size(); i++) {
                GameMeta m = list.get(i);
                drawCard(g, m, x, y, cardW, cardH, t);
                hits.add(new Object[]{new Rectangle(x, y, cardW, cardH), m});
                x += cardW + 10;
                if ((i + 1) % cols == 0) {
                    x = PAD;
                    y += cardH + 10;
                }
            }
            y += 20;
            g.setColor(t.textSub);
            g.setFont(g.getFont().deriveFont(11f));
            g.drawString("右键收藏喜欢的游戏", PAD, y + 12);
            y += 40;
            g.dispose();
            setPreferredSize(new Dimension(w, y));
            revalidate();
        }

        private int drawSectionLabel(Graphics2D g, String s, int y, Theme t) {
            g.setColor(t.textSub);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
            g.drawString(s, PAD + 2, y + 14);
            return y + 26;
        }

        private void drawCard(Graphics2D g, GameMeta m, int x, int y, int cw, int ch, Theme t) {
            g.setColor(t.card);
            g.fillRoundRect(x, y, cw, ch, 14, 14);
            int ix = x + (cw - 52) / 2;
            g.drawImage(icon(m, 52), ix, y + 10, null);
            g.setColor(m.playable() ? t.text : t.textSub);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
            g.drawString(clip(g, m.name, cw - 10), x + cw / 2, y + 80);

            JSONObject sc = scoreCache.get(m.id);
            int best = sc == null ? 0 : sc.optInt("best", 0);
            String sub = m.playable() ? (best > 0 ? "最高 " + best : "来挑战") : "敬请期待";
            g.setFont(g.getFont().deriveFont(11f));
            g.setColor(t.textSub);
            g.drawString(sub, x + cw / 2, y + 98);

            if (isFav(m.id)) {
                g.setColor(Theme.star());
                g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
                g.drawString("★", x + cw - 16, y + 16);
            }
        }

        private void drawMiniCard(Graphics2D g, GameMeta m, int x, int y, Theme t) {
            g.setColor(t.card);
            g.fillRoundRect(x, y, 96, 112, 12, 12);
            g.drawImage(icon(m, 44), x + 26, y + 10, null);
            g.setColor(t.text);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
            g.drawString(clip(g, m.name, 92), x + 48, y + 74);
            JSONObject sc = scoreCache.get(m.id);
            int best = sc == null ? 0 : sc.optInt("best", 0);
            g.setColor(t.textSub);
            g.setFont(g.getFont().deriveFont(10f));
            g.drawString(best > 0 ? "最高 " + best : "未挑战", x + 48, y + 92);
        }

        private String clip(Graphics2D g, String s, int maxW) {
            if (g.getFontMetrics().stringWidth(s) <= maxW) return s;
            while (s.length() > 1 && g.getFontMetrics().stringWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
            return s + "…";
        }
    }

    /** IconDrawable → BufferedImage（复用 Android 端图标代码）。 */
    private BufferedImage icon(GameMeta m, int size) {
        String key = m.id + "@" + size + "@" + Theme.get().dark;
        BufferedImage img = iconCache.get(key);
        if (img == null) {
            img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Drawable d = new IconDrawable(m);
            d.setBounds(new Rect(0, 0, size, size));
            d.draw(new Canvas(g));
            g.dispose();
            iconCache.put(key, img);
        }
        return img;
    }

    // ---------------- 成就 / 设置 / 提示 ----------------

    private void showAchievements() {
        store = Store.loadStore(ctx());
        Theme t = Theme.get();
        JSONArray played = store.optJSONArray("playedIds");
        JPanel box = new JPanel(new GridLayout(0, 1, 0, 0));
        box.setBackground(t.card);
        JLabel stat = new JLabel("累计 " + store.optInt("playsTotal", 0) + " 局 · 玩过 " + played.length()
                + " 款 · 时长 " + (store.optInt("timeSec", 0) / 60) + " 分钟 · 金币 "
                + store.optInt("coins", 0));
        stat.setForeground(t.textSub);
        stat.setBorder(BorderFactory.createEmptyBorder(4, 0, 10, 0));
        box.add(stat);
        for (String[] d : Achievements.DEFS) {
            boolean got = Achievements.has(store, d[0]);
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(t.card);
            row.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, t.cardSub));
            JPanel col = new JPanel();
            col.setLayout(new javax.swing.BoxLayout(col, javax.swing.BoxLayout.Y_AXIS));
            col.setOpaque(false);
            JLabel n = new JLabel(d[1]);
            n.setForeground(got ? t.accent : t.text);
            n.setFont(n.getFont().deriveFont(Font.BOLD, 14f));
            JLabel de = new JLabel(d[2]);
            de.setForeground(t.textSub);
            de.setFont(de.getFont().deriveFont(11f));
            col.add(n);
            col.add(de);
            JLabel st = new JLabel(got ? "已达成" : "未达成");
            st.setForeground(got ? t.accent : t.textSub);
            row.add(col, BorderLayout.CENTER);
            row.add(st, BorderLayout.EAST);
            box.add(row);
        }
        JDialog dlg = new JDialog(this, "我的成就", true);
        JScrollPane sp = new JScrollPane(box);
        sp.setPreferredSize(new Dimension(420, 480));
        dlg.setContentPane(sp);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void showSettings() {
        store = Store.loadStore(ctx());
        Theme t = Theme.get();
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 10));
        p.setBackground(t.card);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 10, 18));

        JCheckBox sound = new JCheckBox("音效（合成提示音）", store.optBoolean("sound", true));
        sound.setForeground(t.text);
        sound.setOpaque(false);
        p.add(sound);

        JCheckBox darkMode = new JCheckBox("深色主题", Theme.get().dark);
        darkMode.setForeground(t.text);
        darkMode.setOpaque(false);
        p.add(darkMode);

        JButton wipe = new JButton("清除全部数据（金币/成就/最高分）");
        wipe.setForeground(Theme.danger());
        wipe.setContentAreaFilled(false);
        wipe.setBorderPainted(false);
        wipe.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wipe.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "将删除本机全部游戏数据（最高分、金币、成就、收藏），且无法恢复。",
                    "确认清除", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.OK_OPTION) {
                new File(ctx().getFilesDir(), "store.json").delete();
                deleteDir(new File(ctx().getFilesDir(), "scores"));
                deleteDir(new File(ctx().getFilesDir(), "logs"));
                toast("已清除全部数据");
                refresh();
            }
        });
        p.add(wipe);

        JLabel about = new JLabel("<html><center>百游口袋 v1.0（PC 端）<br>与 Android 端同一套游戏代码<br>数据仅保存在本机 desktop-data/</center></html>");
        about.setForeground(t.textSub);
        about.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        p.add(about);

        JDialog dlg = new JDialog(this, "设置", true);
        dlg.setContentPane(p);
        dlg.setSize(380, 260);
        dlg.setLocationRelativeTo(this);
        sound.addActionListener(e -> {
            store = Store.loadStore(ctx());
            try {
                store.put("sound", sound.isSelected());
            } catch (Exception ignore) {
            }
            Store.saveStore(ctx(), store);
        });
        darkMode.addActionListener(e -> {
            Theme.get().dark = darkMode.isSelected();
            Theme.get().apply();
            iconCache.clear();
            buildUi();
            refresh();
            dlg.dispose();
        });
        dlg.setVisible(true);
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }

    private JLabel toastLabel;
    private javax.swing.Timer toastTimer;

    public void toast(String msg) {
        if (toastLabel == null) {
            toastLabel = new JLabel("", javax.swing.SwingConstants.CENTER);
            toastLabel.setOpaque(true);
            toastLabel.setBackground(Theme.get().card);
            toastLabel.setForeground(Theme.get().text);
            toastLabel.setFont(toastLabel.getFont().deriveFont(Font.BOLD, 13f));
            toastLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            getLayeredPane().add(toastLabel, javax.swing.JLayeredPane.POPUP_LAYER);
            toastLabel.setVisible(false);
        }
        toastLabel.setText(msg);
        toastLabel.setSize(toastLabel.getPreferredSize());
        toastLabel.setLocation((getWidth() - toastLabel.getWidth()) / 2, getHeight() - 80);
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(2000, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    /** 简易整型矩形（避免与 android.graphics.Rect 混淆的 awt Rectangle 别名）。 */
    private static final class Rectangle extends java.awt.Rectangle {
        Rectangle(int x, int y, int w, int h) {
            super(x, y, w, h);
        }
    }
}
