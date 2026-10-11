package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.Random;

/** g059 残局闯关：三个经典接龙残局，每局只有一张手牌能接上龙；
 *  点对了 +300 进下一局，点错扣命，通关有大奖励。 */
public class CardEndgameGame extends CanvasGame {
    // {龙尾点数, 方向(0=只能-1,1=只能+1), 5张手牌(第一张是唯一解), 答案花色}
    private static final int[][] LEVELS = {
            {9, 0, 8, 10, 2, 5, 12, 0},
            {5, 1, 6, 4, 8, 3, 9, 2},
            {11, 0, 10, 12, 9, 13, 5, 3},
    };

    private int level, lives = 3, shownScore;
    private int tailRank, dir;
    private final int[] hand = new int[5];
    private final int[] handSuit = new int[5];
    private int ansPos = -1;
    private boolean settling;

    public CardEndgameGame() {
        super(360, 560);
        loadLevel(0);
    }

    private void loadLevel(int lv) {
        level = lv;
        tailRank = LEVELS[lv][0];
        dir = LEVELS[lv][1];
        int[] cards = new int[5];
        for (int i = 0; i < 5; i++) cards[i] = LEVELS[lv][2 + i];
        int suit = LEVELS[lv][7];
        // 打乱手牌
        Integer[] idxs = {0, 1, 2, 3, 4};
        java.util.Collections.shuffle(java.util.Arrays.asList(idxs), new Random(lv * 77 + 13));
        for (int i = 0; i < 5; i++) {
            hand[i] = cards[idxs[i]];
            handSuit[i] = (suit + i) % 4;
            if (idxs[i] == 0) ansPos = i;
        }
        settling = false;
    }

    private void startGame() {
        lives = 3;
        shownScore = 0;
        over = false;
        loadLevel(0);
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private float handX(int i) {
        return 14 + i * 68;
    }

    private static final float HAND_Y = 320, CW = 62, CH = 86;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        for (int i = 0; i < 5; i++) {
            if (hit(x, y, handX(i), HAND_Y, CW, CH)) {
                if (i == ansPos) {
                    shownScore += 300;
                    host.setScore(shownScore);
                    beep(1);
                    burst(handX(i) + CW / 2f, HAND_Y + CH / 2f, 0xFF7CFFB2, 12, 5f);
                    popup("妙手！ +300", lw / 2f, 240, 0xFF7CFFB2, 18);
                    settling = true;
                    postDelayedSafe(() -> {
                        settling = false;
                        if (over) return;
                        if (level >= LEVELS.length - 1) {
                            over = true;
                            stopLoop();
                            shownScore += 500;
                            host.setScore(shownScore);
                            popup("残局大师！ +500", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
                            dirty();
                            postDelayedSafe(() -> host.gameOver(shownScore), 900);
                        } else {
                            loadLevel(level + 1);
                            dirty();
                        }
                    }, 800);
                } else {
                    lives--;
                    beep(2);
                    shake(200);
                    if (lives <= 0) {
                        over = true;
                        stopLoop();
                        popup("残局败北", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                        dirty();
                        postDelayedSafe(() -> host.gameOver(shownScore), 850);
                    }
                }
                dirty();
                return;
            }
        }
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E2620);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "残局 " + (level + 1) + "/" + LEVELS.length, 40, 30, 14, 0xFFECECF1, true);
        label(c, dir == 0 ? "规则：只能接比龙尾小 1 的牌" : "规则：只能接比龙尾大 1 的牌",
                lw / 2f, 90, 14, 0xFFFFC24B, true);
        // 龙尾
        CardKit.draw(this, c, tailRank, dir, 140, 150, 80, 108, true);
        label(c, "龙尾", 180, 140, 12, 0xFF8B93A7, false);
        // 手牌
        for (int i = 0; i < 5; i++) {
            CardKit.draw(this, c, hand[i], handSuit[i], handX(i), HAND_Y, CW, CH, true);
        }
        label(c, "选出唯一能接的牌", lw / 2f, 440, 13, 0xFF8B93A7, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
