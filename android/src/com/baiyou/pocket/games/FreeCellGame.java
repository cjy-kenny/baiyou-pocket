package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** g052 空当接龙：8 列牌、4 个空当、4 个花色基础堆。
 *  点牌自动落位（优先基础堆 → 空当），一次一张；
 *  全部上基础堆获胜，无路可走结算。进堆 +15。 */
public class FreeCellGame extends CanvasGame {
    private static final float CW = 42, CH = 58;

    private final Random rnd = new Random();
    private final List<List<Integer>> cols = new ArrayList<>(); // 8 列
    private final int[] free = new int[4];                      // -1 空
    private final int[] foundations = new int[4];
    private int score;
    private boolean settling;

    public FreeCellGame() {
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
        cols.clear();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 52; i++) all.add(i);
        Collections.shuffle(all, rnd);
        int[] sizes = {7, 7, 7, 7, 6, 6, 6, 6};
        int p = 0;
        for (int i = 0; i < 8; i++) {
            List<Integer> col = new ArrayList<>();
            for (int j = 0; j < sizes[i]; j++) col.add(all.get(p++));
            cols.add(col);
        }
        java.util.Arrays.fill(free, -1);
        java.util.Arrays.fill(foundations, 0);
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

    private float colX(int i) {
        return 6 + i * 44;
    }

    private static final float COL_Y = 130, CELL_H = 24;

    private float freeX(int i) {
        return 6 + i * 46;
    }

    private float foundX(int i) {
        return 198 + i * 42;
    }

    private boolean tryFoundation(int card) {
        int s = suitOf(card);
        if (rankOf(card) == foundations[s] + 1) {
            foundations[s]++;
            addScore(15);
            return true;
        }
        return false;
    }

    private void addScore(int v) {
        score += v;
        host.setScore(score);
    }

    /** 无任何牌能进基础堆 = 卡死。 */
    private boolean stuck() {
        for (List<Integer> col : cols) {
            if (!col.isEmpty()) {
                int card = col.get(col.size() - 1);
                if (rankOf(card) == foundations[suitOf(card)] + 1) return false;
            }
        }
        for (int f : free) {
            if (f != -1 && rankOf(f) == foundations[suitOf(f)] + 1) return false;
        }
        return true;
    }

    private boolean won() {
        return foundations[0] == 13 && foundations[1] == 13
                && foundations[2] == 13 && foundations[3] == 13;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 空当
        for (int i = 0; i < 4; i++) {
            if (hit(x, y, freeX(i), 64, CW, CH) && free[i] != -1) {
                int card = free[i];
                if (tryFoundation(card)) {
                    free[i] = -1;
                    beep(1);
                } else {
                    beep(2);
                }
                after();
                return;
            }
        }
        // 列顶牌
        for (int i = 0; i < 8; i++) {
            List<Integer> col = cols.get(i);
            if (col.isEmpty()) continue;
            float topY = COL_Y + (col.size() - 1) * CELL_H;
            if (hit(x, y, colX(i), topY, CW, CH)) {
                int card = col.get(col.size() - 1);
                if (tryFoundation(card)) {
                    col.remove(col.size() - 1);
                    beep(1);
                } else {
                    boolean placed = false;
                    for (int f = 0; f < 4; f++) {
                        if (free[f] == -1) {
                            free[f] = card;
                            col.remove(col.size() - 1);
                            placed = true;
                            beep(0);
                            break;
                        }
                    }
                    if (!placed) beep(2);
                }
                after();
                return;
            }
        }
        dirty();
    }

    private void after() {
        if (won()) {
            settling = true;
            over = true;
            stopLoop();
            addScore(500);
            popup("全线贯通！", lw / 2f, lh / 2f, 0xFFFFC24B, 26);
            burst(lw / 2f, lh / 2f, 0xFFFFC24B, 30, 9f);
            beep(3);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 1000);
            return;
        }
        if (stuck()) {
            settling = true;
            over = true;
            stopLoop();
            beep(2);
            popup("无路可走了 · 进堆 " + (score / 15) + " 张", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 900);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF182430);
        for (int i = 0; i < 4; i++) {
            CardKit.slot(this, c, freeX(i), 64, CW, CH, 0x44203648);
            if (free[i] != -1) {
                CardKit.draw(this, c, rankOf(free[i]), suitOf(free[i]), freeX(i), 64, CW, CH, true);
            }
        }
        for (int i = 0; i < 4; i++) {
            CardKit.slot(this, c, foundX(i), 64, CW, CH, 0x44203648);
            if (foundations[i] > 0) {
                CardKit.draw(this, c, i * 13 + foundations[i] - 1, i, foundX(i), 64, CW, CH, true);
            } else {
                label(c, String.valueOf(CardKit.SUITS.charAt(i)), foundX(i) + CW / 2f,
                        64 + CH / 2f, 16, 0x66FFFFFF, true);
            }
        }
        for (int i = 0; i < 8; i++) {
            List<Integer> col = cols.get(i);
            if (col.isEmpty()) {
                CardKit.slot(this, c, colX(i), COL_Y, CW, CH, 0x33203648);
                continue;
            }
            for (int j = 0; j < col.size(); j++) {
                int card = col.get(j);
                float cy = COL_Y + j * CELL_H;
                boolean top = j == col.size() - 1;
                if (top) {
                    CardKit.draw(this, c, rankOf(card), suitOf(card), colX(i), cy, CW, CH, true);
                } else {
                    // 压扁的牌面（只画上半）
                    CardKit.draw(this, c, rankOf(card), suitOf(card), colX(i), cy, CW, CH * 0.55f, true);
                }
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
