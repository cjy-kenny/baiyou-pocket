package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** g056 大小接龙：从手牌里接出一条点数 ±1 的长龙（可升可降），
 *  接不上就摸牌；手牌与牌堆清空即胜，接龙越长分越高。 */
public class RankChainGame extends CanvasGame {
    private final Random rnd = new Random();
    private final List<Integer> deck = new ArrayList<>();
    private final List<Integer> hand = new ArrayList<>();
    private int chainRank;      // 龙尾点数
    private int chainLen, score;
    private boolean settling;

    public RankChainGame() {
        super(360, 560);
        startRound();
    }

    private void startRound() {
        deck.clear();
        for (int i = 0; i < 52; i++) deck.add(i);
        Collections.shuffle(deck, rnd);
        hand.clear();
        for (int i = 0; i < 6; i++) hand.add(deck.remove(deck.size() - 1));
        chainRank = 7;
        chainLen = 0;
        score = 0;
        settling = false;
        over = false;
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
        host.setScore(0);
    }

    private float handX(int i) {
        return 12 + i * 58;
    }

    private static final float HAND_Y = 360, CW = 54, CH = 76;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        for (int i = hand.size() - 1; i >= 0; i--) {
            if (hit(x, y, handX(i), HAND_Y, CW, CH)) {
                int card = hand.get(i);
                int rank = card % 13 + 1;
                if (Math.abs(rank - chainRank) == 1) {
                    chainRank = rank;
                    chainLen++;
                    score += 10 * chainLen;
                    host.setScore(score);
                    hand.remove(i);
                    beep(1);
                    burst(180, 210, 0xFF7CFFB2, 8, 4f);
                    if (hand.isEmpty() && deck.isEmpty()) {
                        score += 500;
                        host.setScore(score);
                        win();
                        return;
                    }
                    if (hand.size() < 6 && !deck.isEmpty()) {
                        hand.add(deck.remove(deck.size() - 1));
                    }
                } else {
                    beep(2);
                    shake(140);
                }
                dirty();
                return;
            }
        }
        // 摸牌堆
        if (hit(x, y, 140, 470, 80, 60) && !deck.isEmpty()) {
            hand.add(deck.remove(deck.size() - 1));
            beep(0);
            dirty();
        }
        checkStuck();
    }

    /** 死局检测：牌堆摸空且手里没有能接的牌 → 结算。 */
    private void checkStuck() {
        if (over || settling || !deck.isEmpty()) return;
        for (int card : hand) {
            int rank = card % 13 + 1;
            if (Math.abs(rank - chainRank) == 1) return;
        }
        over = true;
        stopLoop();
        popup("无牌可接 · 接龙 " + chainLen + " 张", lw / 2f, lh / 2f, 0xFFECECF1, 17);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 850);
    }

    private void win() {
        settling = true;
        over = true;
        stopLoop();
        popup("清空！ +500", lw / 2f, lh / 2f, 0xFFFFC24B, 24);
        burst(lw / 2f, lh / 2f, 0xFFFFC24B, 26, 8f);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 950);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20262E);
        label(c, "接 ±1 的长龙 · 已接 " + chainLen + " 张", lw / 2f, 80, 14, 0xFF9AA0AC, false);
        // 龙头
        CardKit.draw(this, c, chainRank, 0, 130, 150, CW + 20, CH + 14, true);
        label(c, "龙尾", 167, 140, 12, 0xFF8B93A7, false);
        // 手牌
        for (int i = 0; i < hand.size(); i++) {
            int card = hand.get(i);
            int rank = card % 13 + 1;
            boolean ok = Math.abs(rank - chainRank) == 1;
            CardKit.draw(this, c, rank, card / 13, handX(i), HAND_Y, CW, CH, true);
            if (ok) {
                stroke.setColor(0xFF7CFFB2);
                stroke.setStrokeWidth(2);
                rf.set(handX(i) - 2, HAND_Y - 2, handX(i) + CW + 2, HAND_Y + CH + 2);
                c.drawRoundRect(rf, 6, 6, stroke);
            }
        }
        rr(c, 140, 470, 220, 530, 10, 0xFF2A2D37);
        label(c, "摸牌 " + deck.size(), 180, 500, 15, 0xFF8B93A7, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
