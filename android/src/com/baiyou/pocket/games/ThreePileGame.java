package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** g054 三堆翻牌：三摞牌各自翻出顶张，顶张点数相同的两张自动成对消除；
 *  清空三摞即胜（+500），翻完仍有剩余按剩余张数扣分。 */
public class ThreePileGame extends CanvasGame {
    private final Random rnd = new Random();
    private final List<List<Integer>> piles = new ArrayList<>();
    private final boolean[] revealed = new boolean[3];
    private int score;
    private boolean settling;

    public ThreePileGame() {
        super(360, 560);
        deal();
    }

    private void deal() {
        piles.clear();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < 52; i++) all.add(i);
        Collections.shuffle(all, rnd);
        for (int i = 0; i < 3; i++) {
            List<Integer> p = new ArrayList<>();
            for (int j = 0; j < 10; j++) p.add(all.get(i * 10 + j));
            piles.add(p);
        }
        java.util.Arrays.fill(revealed, false);
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

    private float pileX(int i) {
        return 30 + i * 110;
    }

    private static final float PILE_Y = 150, CW = 80, CH = 110;

    private void checkPairs() {
        for (int a = 0; a < 3; a++)
            for (int b = a + 1; b < 3; b++) {
                if (!revealed[a] || !revealed[b]) continue;
                if (piles.get(a).isEmpty() || piles.get(b).isEmpty()) continue;
                int ca = piles.get(a).get(piles.get(a).size() - 1);
                int cb = piles.get(b).get(piles.get(b).size() - 1);
                if (ca % 13 == cb % 13) {
                    piles.get(a).remove(piles.get(a).size() - 1);
                    piles.get(b).remove(piles.get(b).size() - 1);
                    revealed[a] = revealed[b] = false;
                    score += 30;
                    host.setScore(score);
                    beep(1);
                    popup("配对 +30", 180, 130, 0xFF7CFFB2, 18);
                    burst(pileX(a) + CW / 2f, PILE_Y + CH / 2f, 0xFF7CFFB2, 10, 5f);
                    burst(pileX(b) + CW / 2f, PILE_Y + CH / 2f, 0xFF7CFFB2, 10, 5f);
                }
            }
        boolean empty = piles.get(0).isEmpty() && piles.get(1).isEmpty() && piles.get(2).isEmpty();
        if (empty && !over) {
            score += 500;
            host.setScore(score);
            win();
        }
    }

    private void win() {
        settling = true;
        over = true;
        stopLoop();
        popup("清空三摞！", lw / 2f, lh / 2f, 0xFFFFC24B, 26);
        burst(lw / 2f, lh / 2f, 0xFFFFC24B, 30, 9f);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 1000);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        for (int i = 0; i < 3; i++) {
            if (hit(x, y, pileX(i), PILE_Y, CW, CH)) {
                if (!revealed[i] && !piles.get(i).isEmpty()) {
                    revealed[i] = true;
                    beep(0);
                    checkPairs();
                } else if (revealed[i]) {
                    // 再点已亮牌堆：翻下一张（放弃当前顶张）
                    revealed[i] = false;
                    beep(0);
                }
                dirty();
                return;
            }
        }
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF241E28);
        label(c, "点堆翻牌 · 顶张同点数自动配对", lw / 2f, 80, 13, 0xFFC9AED0, false);
        for (int i = 0; i < 3; i++) {
            List<Integer> p = piles.get(i);
            if (p.isEmpty()) {
                CardKit.slot(this, c, pileX(i), PILE_Y, CW, CH, 0x33203648);
                label(c, "空", pileX(i) + CW / 2f, PILE_Y + CH / 2f, 16, 0x66FFFFFF, true);
            } else if (revealed[i]) {
                CardKit.draw(this, c, p.get(p.size() - 1) % 13 + 1,
                        p.get(p.size() - 1) / 13, pileX(i), PILE_Y, CW, CH, true);
                label(c, "剩 " + p.size(), pileX(i) + CW / 2f, PILE_Y + CH + 22, 12, 0xFF8B93A7, false);
            } else {
                CardKit.draw(this, c, 0, 0, pileX(i), PILE_Y, CW, CH, false);
                label(c, "剩 " + p.size(), pileX(i) + CW / 2f, PILE_Y + CH + 22, 12, 0xFF8B93A7, false);
            }
        }
        label(c, "再点已亮的堆 = 跳过该张", lw / 2f, lh - 30, 12, 0xFF8B93A7, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
