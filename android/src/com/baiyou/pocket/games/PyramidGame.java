package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** g058 牌塔叠叠：金字塔牌阵，点两张点数相加为 13 的亮牌拆掉，
 *  K 可单独拆；牌堆与弃牌堆顶也能配。清空金字塔即胜。 */
public class PyramidGame extends CanvasGame {
    private static final float CW = 44, CH = 60;

    private final Random rnd = new Random();
    private final List<Integer> pyramid = new ArrayList<>();   // 28 张，idx0 在顶
    private final boolean[] removed = new boolean[28];
    private final List<Integer> stock = new ArrayList<>();
    private final List<Integer> waste = new ArrayList<>();
    private int selKind;   // 0 无 1 金字塔 2 牌堆 3 弃牌堆
    private int selIdx = -1;
    private int score;
    private boolean settling;

    public PyramidGame() {
        super(360, 560);
        deal();
    }

    private int rankOf(int card) {
        return card % 13 + 1;
    }

    private void deal() {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 52; i++) all.add(i);
        Collections.shuffle(all, rnd);
        pyramid.clear();
        for (int i = 0; i < 28; i++) pyramid.add(all.get(i));
        for (boolean b : removed) b = false;
        stock.clear();
        for (int i = 28; i < 52; i++) stock.add(all.get(i));
        waste.clear();
        selKind = 0;
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

    /** 金字塔第 idx 张的位置（0 顶行 1 张 … 6 底行 7 张）。 */
    private int rowOf(int idx) {
        int row = 0;
        while (idx >= row + 1) {
            idx -= row + 1;
            row++;
        }
        return row;
    }

    private int colOf(int idx) {
        int row = rowOf(idx);
        return idx - (row * (row + 1) / 2);
    }

    /** 该张是否未被压住（下一行相邻两张都已移除或不存在）。 */
    private boolean exposed(int idx) {
        int row = rowOf(idx), col = colOf(idx);
        if (row == 6) return true;
        int below = (row + 1) * (row + 2) / 2 + col;
        boolean b1 = below >= 28 || removed[below];
        boolean b2 = below + 1 >= 28 || removed[below + 1];
        return b1 && b2;
    }

    private float cardX(int idx) {
        int row = rowOf(idx);
        float span = row * CW;
        return lw / 2f - span / 2f + colOf(idx) * CW;
    }

    private float cardY(int idx) {
        return 100 + rowOf(idx) * 52;
    }

    private void removePair() {
        score += 30;
        host.setScore(score);
        beep(1);
        popup("合计 13！ +30", lw / 2f, 84, 0xFF7CFFB2, 16);
    }

    private boolean take(int kind, int idx) {
        if (kind == 1) {
            if (removed[idx] || !exposed(idx)) return false;
            removed[idx] = true;
            return true;
        }
        return true; // 牌堆/弃牌堆由调用方移除
    }

    /** 取得当前位置的牌值；kind：1金字塔 2牌堆顶 3弃牌堆顶。 */
    private Integer cardAt(int kind, int idx) {
        if (kind == 1) {
            if (removed[idx]) return null;
            return pyramid.get(idx);
        }
        if (kind == 2) return stock.isEmpty() ? null : stock.get(stock.size() - 1);
        return waste.isEmpty() ? null : waste.get(waste.size() - 1);
    }

    private void removeAt(int kind, int idx) {
        if (kind == 1) removed[idx] = true;
        else if (kind == 2) stock.remove(stock.size() - 1);
        else waste.remove(waste.size() - 1);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 牌堆
        if (hit(x, y, 20, 452, CW, CH)) {
            if (!stock.isEmpty()) {
                int card = stock.remove(stock.size() - 1);
                waste.add(card);
                selKind = 0;
                beep(0);
            } else if (!waste.isEmpty()) {
                Collections.reverse(waste);
                stock.addAll(waste);
                waste.clear();
                score = Math.max(0, score - 20);
                host.setScore(score);
                beep(0);
            }
            dirty();
            return;
        }
        // 弃牌堆顶
        if (hit(x, y, 110, 452, CW, CH) && !waste.isEmpty()) {
            trySelect(3, -1);
            return;
        }
        // 金字塔
        for (int idx = 0; idx < 28; idx++) {
            if (hit(x, y, cardX(idx), cardY(idx), CW - 4, 48)) {
                trySelect(1, idx);
                return;
            }
        }
        dirty();
    }

    private void trySelect(int kind, int idx) {
        Integer card = cardAt(kind, idx);
        if (card == null) return;
        boolean exposed = kind != 1 || exposed(idx);
        if (!exposed) {
            beep(2);
            dirty();
            return;
        }
        if (selKind == 0) {
            // K 可以单拆
            if (rankOf(card) == 13) {
                removeAt(kind, idx);
                score += 30;
                host.setScore(score);
                beep(1);
                popup("K！ +30", lw / 2f, 84, 0xFF7CFFB2, 16);
                checkWin();
                dirty();
                return;
            }
            selKind = kind;
            selIdx = idx;
            beep(0);
            dirty();
            return;
        }
        // 已有一张 → 尝试配 13
        Integer first = cardAt(selKind, selIdx);
        if (first != null && rankOf(first) + rankOf(card) == 13) {
            removeAt(selKind, selIdx);
            removeAt(kind, idx);
            removePair();
            selKind = 0;
            checkWin();
            dirty();
            return;
        }
        // 换选
        selKind = kind;
        selIdx = idx;
        beep(0);
        dirty();
    }

    private void checkWin() {
        boolean all = true;
        for (boolean b : removed) if (!b) all = false;
        if (all) {
            settling = true;
            over = true;
            stopLoop();
            score += 500;
            host.setScore(score);
            popup("金字塔拆完！ +500", lw / 2f, lh / 2f, 0xFFFFC24B, 24);
            burst(lw / 2f, lh / 2f, 0xFFFFC24B, 30, 9f);
            beep(3);
            postDelayedSafe(() -> host.gameOver(score), 1000);
        }
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF26201A);
        for (int idx = 0; idx < 28; idx++) {
            if (removed[idx]) continue;
            boolean exp = exposed(idx);
            if (exp) {
                CardKit.draw(this, c, rankOf(pyramid.get(idx)), pyramid.get(idx) / 13,
                        cardX(idx), cardY(idx), CW - 4, 48, true);
                if (selKind == 1 && selIdx == idx) {
                    stroke.setColor(0xFFFFC24B);
                    stroke.setStrokeWidth(2);
                    rf.set(cardX(idx) - 2, cardY(idx) - 2, cardX(idx) + CW - 2, cardY(idx) + 50);
                    c.drawRoundRect(rf, 6, 6, stroke);
                }
            } else {
                CardKit.draw(this, c, 0, 0, cardX(idx), cardY(idx), CW - 4, 48, false);
            }
        }
        CardKit.slot(this, c, 20, 452, CW, CH, 0x443A3222);
        if (!stock.isEmpty()) CardKit.draw(this, c, 0, 0, 20, 452, CW, CH, false);
        CardKit.slot(this, c, 110, 452, CW, CH, 0x443A3222);
        if (!waste.isEmpty()) {
            CardKit.draw(this, c, rankOf(waste.get(waste.size() - 1)),
                    waste.get(waste.size() - 1) / 13, 110, 452, CW, CH, true);
            if (selKind == 3) {
                stroke.setColor(0xFFFFC24B);
                stroke.setStrokeWidth(2);
                rf.set(108, 450, 110 + CW + 2, 452 + CH + 2);
                c.drawRoundRect(rf, 6, 6, stroke);
            }
        }
        label(c, "凑 13 拆塔 · K 单拆", lw / 2f, 66, 13, 0xFFC9B896, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
