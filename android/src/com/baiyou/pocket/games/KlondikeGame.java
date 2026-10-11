package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** g051 经典接龙（Klondike 精简版）：点牌选中、点目标落牌；
 *  花色基础堆 A→K，牌列红黑相间降序叠放，牌堆翻单张；
 *  四堆齐 K 即胜。进基础堆 +20，翻开暗牌 +5。 */
public class KlondikeGame extends CanvasGame {
    private static final float CW = 46, CH = 62;

    private final Random rnd = new Random();
    private final List<List<Integer>> tab = new ArrayList<>();   // 7 列
    private final boolean[][] faceUp = new boolean[7][];
    private final List<Integer> deck = new ArrayList<>();
    private final List<Integer> waste = new ArrayList<>();
    private final int[] foundations = new int[4];                // 各花色已放张数
    private int selSource;   // 0 无 1 牌堆弃牌区 2 牌列
    private int selCol, selIdx;
    private int score;
    private boolean settling;

    public KlondikeGame() {
        super(360, 560);
        deal();
    }

    private int suitOf(int card) {
        return card / 13;
    }

    private int rankOf(int card) {
        return card % 13 + 1;
    }

    private void deal() {
        tab.clear();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 52; i++) all.add(i);
        Collections.shuffle(all, rnd);
        for (int i = 0; i < 7; i++) {
            List<Integer> col = new ArrayList<>();
            for (int j = 0; j <= i; j++) col.add(all.remove(all.size() - 1));
            tab.add(col);
        }
        // faceUp 结构重建
        for (int i = 0; i < 7; i++) {
            faceUp[i] = new boolean[tab.get(i).size()];
            for (int j = 0; j < faceUp[i].length; j++) faceUp[i][j] = j == i;
        }
        deck.clear();
        deck.addAll(all);
        waste.clear();
        java.util.Arrays.fill(foundations, 0);
        selSource = 0;
        score = 0;
        settling = false;
        over = false;
        dirty();
    }

    @Override
    public void restartGame() {
        deal();
        host.setScore(0);
    }

    private float tabX(int i) {
        return 6 + i * 50;
    }

    private static final float TAB_Y = 150;

    private float foundX(int i) {
        return 140 + i * 52;
    }

    private boolean isRed(int card) {
        return CardKit.isRed(suitOf(card));
    }

    private boolean canTab(int card, int col) {
        List<Integer> p = tab.get(col);
        if (p.isEmpty()) return rankOf(card) == 13;
        int top = p.get(p.size() - 1);
        return isRed(card) != isRed(top) && rankOf(card) == rankOf(top) - 1;
    }

    private void addScore(int v) {
        score += v;
        host.setScore(score);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 牌堆
        if (hit(x, y, 8, 70, CW, CH)) {
            if (!deck.isEmpty()) {
                waste.add(deck.remove(deck.size() - 1));
                selSource = 0;
                beep(0);
            } else if (!waste.isEmpty()) {
                Collections.reverse(waste);
                deck.addAll(waste);
                waste.clear();
                addScore(-10);
                selSource = 0;
                beep(0);
            }
            dirty();
            return;
        }
        // 基础堆（放牌）
        for (int i = 0; i < 4; i++) {
            if (hit(x, y, foundX(i), 70, CW, CH)) {
                tryToFoundation(i);
                return;
            }
        }
        // 弃牌堆
        if (hit(x, y, 74, 70, CW, CH)) {
            if (!waste.isEmpty()) {
                if (selSource == 1) selSource = 0;
                else {
                    selSource = 1;
                    selCol = -1;
                    selIdx = -1;
                    beep(0);
                }
            }
            dirty();
            return;
        }
        // 牌列
        for (int col = 0; col < 7; col++) {
            List<Integer> p = tab.get(col);
            for (int idx = 0; idx < p.size(); idx++) {
                float cy = TAB_Y + idx * 16;
                if (hit(x, y, tabX(col), cy, CW, CH)) {
                    if (!faceUp[col][idx]) return;
                    handleTabTap(col, idx);
                    return;
                }
            }
            // 空列区域
            if (p.isEmpty() && hit(x, y, tabX(col), TAB_Y, CW, CH)) {
                handleTabTap(col, -1);
                return;
            }
        }
    }

    private void handleTabTap(int col, int idx) {
        List<Integer> p = tab.get(col);
        if (idx >= 0 && idx < p.size() - 1 && !faceUp[col][idx + 1]) idx = p.size() - 1;
        if (selSource == 0) {
            if (idx >= 0 && faceUp[col][idx]) {
                selSource = 2;
                selCol = col;
                selIdx = idx;
                beep(0);
            }
            dirty();
            return;
        }
        // 已有选中 → 尝试落到此列
        if (selSource == 2 && selCol == col) {
            selSource = 0;
            dirty();
            return;
        }
        boolean fromTab = selSource == 2;
        int fromCol = selCol;
        boolean ok = false;
        if (selSource == 1 && !waste.isEmpty()) {
            int card = waste.get(waste.size() - 1);
            if (canTab(card, col)) {
                p.add(card);
                waste.remove(waste.size() - 1);
                ok = true;
            }
        } else if (fromTab) {
            List<Integer> src = tab.get(fromCol);
            int card = src.get(selIdx);
            if (canTab(card, col)) {
                for (int i = selIdx; i < src.size(); i++) {
                    p.add(src.get(i));
                }
                while (src.size() > selIdx) src.remove(src.size() - 1);
                ok = true;
            }
        }
        if (ok) {
            beep(1);
            selSource = 0;
            syncFaceUp(col);
            if (fromTab) flipTop(fromCol);
            if (foundationsFull()) {
                win();
                return;
            }
        } else {
            beep(2);
        }
        dirty();
    }

    private void tryToFoundation(int f) {
        int need = foundations[f] + 1;
        Integer card = null;
        if (selSource == 1 && !waste.isEmpty() && suitOf(waste.get(waste.size() - 1)) == f
                && rankOf(waste.get(waste.size() - 1)) == need) {
            card = waste.remove(waste.size() - 1);
        } else if (selSource == 2) {
            List<Integer> src = tab.get(selCol);
            if (selIdx == src.size() - 1 && suitOf(src.get(selIdx)) == f
                    && rankOf(src.get(selIdx)) == need) {
                card = src.remove(src.size() - 1);
                flipTop(selCol);
            }
        }
        if (card != null) {
            foundations[f]++;
            addScore(20);
            selSource = 0;
            beep(1);
            burst(foundX(f) + CW / 2f, 100, 0xFF7CFFB2, 8, 4f);
            if (foundationsFull()) {
                win();
                return;
            }
        } else {
            beep(2);
        }
        dirty();
    }

    /** 牌列尺寸变化后同步 faceUp 标记数组（新牌默认正面）。 */
    private void syncFaceUp(int col) {
        List<Integer> p = tab.get(col);
        if (faceUp[col].length == p.size()) return;
        boolean[] nu = new boolean[p.size()];
        for (int i = 0; i < nu.length; i++)
            nu[i] = i < faceUp[col].length && faceUp[col][i];
        faceUp[col] = nu;
    }

    private void flipTop(int col) {
        List<Integer> p = tab.get(col);
        syncFaceUp(col);
        if (!p.isEmpty() && !faceUp[col][p.size() - 1]) {
            faceUp[col][p.size() - 1] = true;
            addScore(5);
        }
    }

    private boolean foundationsFull() {
        return foundations[0] == 13 && foundations[1] == 13
                && foundations[2] == 13 && foundations[3] == 13;
    }

    private void win() {
        settling = true;
        over = true;
        stopLoop();
        addScore(500);
        popup("大满贯！", lw / 2f, lh / 2f, 0xFFFFC24B, 26);
        burst(lw / 2f, lh / 2f, 0xFFFFC24B, 30, 9f);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 1000);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1B3A28);
        // 牌堆/弃牌/基础堆
        CardKit.slot(this, c, 8, 70, CW, CH, 0x55203A2A);
        if (!deck.isEmpty()) CardKit.draw(this, c, 0, 0, 8, 70, CW, CH, false);
        if (!waste.isEmpty()) {
            int card = waste.get(waste.size() - 1);
            drawCard(c, card, 74, 70, selSource == 1);
        } else {
            CardKit.slot(this, c, 74, 70, CW, CH, 0x55203A2A);
        }
        for (int i = 0; i < 4; i++) {
            CardKit.slot(this, c, foundX(i), 70, CW, CH, 0x55203A2A);
            if (foundations[i] > 0) {
                drawCard(c, i * 13 + foundations[i] - 1, foundX(i), 70, false);
            } else {
                label(c, String.valueOf(CardKit.SUITS.charAt(i)), foundX(i) + CW / 2f,
                        70 + CH / 2f, 18, 0x66FFFFFF, true);
            }
        }
        // 牌列
        for (int col = 0; col < 7; col++) {
            List<Integer> p = tab.get(col);
            if (p.isEmpty()) {
                CardKit.slot(this, c, tabX(col), TAB_Y, CW, CH, 0x44203A2A);
                continue;
            }
            for (int idx = 0; idx < p.size(); idx++) {
                float cy = TAB_Y + idx * 16;
                boolean sel = selSource == 2 && selCol == col && selIdx <= idx && idx < p.size();
                if (!faceUp[col][idx]) {
                    CardKit.draw(this, c, 0, 0, tabX(col), cy, CW, CH, false);
                } else {
                    drawCard(c, p.get(idx), tabX(col), cy, sel);
                }
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private void drawCard(Canvas c, int card, float x, float y, boolean sel) {
        CardKit.draw(this, c, rankOf(card), suitOf(card), x, y, CW, CH, true);
        if (sel) {
            stroke.setColor(0xFFFFC24B);
            stroke.setStrokeWidth(2);
            rf.set(x - 2, y - 2, x + CW + 2, y + CH + 2);
            c.drawRoundRect(rf, 6, 6, stroke);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
