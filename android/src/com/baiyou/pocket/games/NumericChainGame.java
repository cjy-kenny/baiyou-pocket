package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** g053 数字牌接龙：手里 5 张牌往四个槽位上接（±1 点数），
 *  无牌可接点牌堆补一张；60 秒内接得越多分越高。 */
public class NumericChainGame extends CanvasGame {
    private static final long ROUND_MS = 60000;

    private final Random rnd = new Random();
    private final List<Integer> deck = new ArrayList<>();
    private final List<Integer> hand = new ArrayList<>();
    private final int[] slots = new int[4];   // 槽顶点数（1..13）
    private int placed, score;
    private long roundEnd;
    private int selHand = -1;
    private boolean settling;

    public NumericChainGame() {
        super(360, 560);
        startRound();
    }

    private void startRound() {
        deck.clear();
        for (int i = 0; i < 52; i++) deck.add(i);
        Collections.shuffle(deck, rnd);
        hand.clear();
        for (int i = 0; i < 5; i++) hand.add(drawCard());
        slots[0] = 6;
        slots[1] = 8;
        slots[2] = 10;
        slots[3] = 3;
        placed = 0;
        score = 0;
        roundEnd = now() + ROUND_MS;
        selHand = -1;
        settling = false;
        over = false;
        startLoop(100);
        dirty();
    }

    private int drawCard() {
        if (deck.isEmpty()) return -1;
        return deck.remove(deck.size() - 1);
    }

    @Override
    public void restartGame() {
        startRound();
        host.setScore(0);
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(100);
    }

    private float handX(int i) {
        return 20 + i * 66;
    }

    private static final float HAND_Y = 380, CW = 60, CH = 82;

    private float slotX(int i) {
        return 20 + i * 86;
    }

    private static final float SLOT_Y = 170;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 手牌
        for (int i = hand.size() - 1; i >= 0; i--) {
            if (hand.get(i) == -1) continue;
            if (hit(x, y, handX(i), HAND_Y, CW, CH)) {
                selHand = i;
                beep(0);
                dirty();
                return;
            }
        }
        // 槽位
        for (int s = 0; s < 4; s++) {
            if (hit(x, y, slotX(s), SLOT_Y, CW, CH)) {
                if (selHand >= 0) {
                    int card = hand.get(selHand);
                    int rank = card % 13 + 1;
                    if (Math.abs(rank - slots[s]) == 1) {
                        slots[s] = rank;
                        hand.set(selHand, drawCard());
                        placed++;
                        score += 10;
                        host.setScore(score);
                        beep(1);
                        burst(slotX(s) + CW / 2f, SLOT_Y + CH / 2f, 0xFF7CFFB2, 8, 4f);
                        if (hand.stream().allMatch(cc -> cc == -1)) {
                            // 手里全空：补一手
                            for (int i2 = 0; i2 < hand.size(); i2++) hand.set(i2, drawCard());
                        }
                    } else {
                        beep(2);
                    }
                    selHand = -1;
                }
                dirty();
                return;
            }
        }
        // 牌堆补牌（弃掉当前选中）
        if (hit(x, y, 140, 470, 80, 60)) {
            if (selHand >= 0) {
                hand.set(selHand, drawCard());
                selHand = -1;
                beep(0);
            }
            dirty();
        }
    }

    @Override
    protected void onTick() {
        // 死局检测：牌堆摸空且手里没有任何能接的牌 → 提前结束
        if (!over && !settling && deck.isEmpty()) {
            boolean any = false;
            for (int card : hand) {
                if (card == -1) continue;
                int rank = card % 13 + 1;
                for (int s = 0; s < 4; s++)
                    if (Math.abs(rank - slots[s]) == 1) any = true;
            }
            if (!any) {
                over = true;
                stopLoop();
                beep(3);
                popup("无牌可接 · 接龙 " + placed + " 张", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                dirty();
                postDelayedSafe(() -> host.gameOver(score), 800);
                return;
            }
        }
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("接龙 " + placed + " 张", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1F2430);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "已接 " + placed + " 张", 40, 30, 14, 0xFFECECF1, true);
        for (int s = 0; s < 4; s++) {
            CardKit.slot(this, c, slotX(s), SLOT_Y, CW, CH, 0x44203648);
            // 槽顶牌
            CardKit.draw(this, c, slots[s], 0, slotX(s), SLOT_Y, CW, CH, true);
            label(c, "±1", slotX(s) + CW / 2f, SLOT_Y - 10, 11, 0xFF8B93A7, false);
        }
        for (int i = 0; i < hand.size(); i++) {
            int card = hand.get(i);
            if (card == -1) continue;
            CardKit.draw(this, c, card % 13 + 1, card / 13, handX(i), HAND_Y, CW, CH, true);
            if (i == selHand) {
                stroke.setColor(0xFFFFC24B);
                stroke.setStrokeWidth(2);
                rf.set(handX(i) - 2, HAND_Y - 2, handX(i) + CW + 2, HAND_Y + CH + 2);
                c.drawRoundRect(rf, 6, 6, stroke);
            }
        }
        rr(c, 140, 470, 220, 530, 10, 0xFF2A2D37);
        label(c, "换一张", 180, 500, 16, 0xFF8B93A7, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
