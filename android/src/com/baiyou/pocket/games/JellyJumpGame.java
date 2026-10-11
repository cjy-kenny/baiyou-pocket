package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g040 果冻跳跳：按住蓄力（力度条），松手起跳到下一块平台；
 *  落点在平台上得分并续跳，落空坠地结束。 */
public class JellyJumpGame extends CanvasGame {
    private static final float GROUND = 500;

    private final Random rnd = new Random();
    private float px, py, vx, vy;
    private float platX, platW;
    private float charge;
    private boolean holding, flying;
    private int score, bestChain;
    private boolean dying;

    public JellyJumpGame() {
        super(360, 560);
        startRound();
    }

    private void startRound() {
        px = 60;
        py = GROUND - 14;
        vx = vy = 0;
        newPlatform();
        charge = 0;
        holding = flying = false;
        score = 0;
        dying = false;
        over = false;
        startLoop(16);
        dirty();
    }

    private void newPlatform() {
        platW = Math.max(54, 100 - score * 2);
        platX = rand(120, lw - platW - 16);
    }

    @Override
    public void restartGame() {
        startRound();
        host.setScore(0);
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying) return;
        if (action == 0 && !flying) {
            holding = true;
            charge = 0;
        } else if (action == 1 && holding) {
            holding = false;
            flying = true;
            vx = 3.4f + charge * 6.4f;
            vy = -8.5f - charge * 5.5f;
            beep(0);
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (holding) {
            charge = Math.min(1f, charge + 0.024f);
        }
        if (flying) {
            vy += 0.32f;
            px += vx;
            py += vy;
            // 落到平台
            if (vy > 0 && px > platX - 10 && px < platX + platW + 10
                    && py + 12 > GROUND - 90 && py + 12 < GROUND - 60 + vy) {
                py = GROUND - 90 - 12;
                flying = false;
                score++;
                bestChain = Math.max(bestChain, score);
                host.setScore(score * 50);
                beep(score % 3 == 0 ? 3 : 1);
                burst(px, py + 10, 0xFF7CFFB2, 10, 4f);
                newPlatform();
            }
            // 落地失败
            if (py > GROUND - 10) {
                dying = true;
                over = true;
                stopLoop();
                shake(300);
                burst(px, GROUND - 10, 0xFFE8574F, 18, 6f);
                beep(2);
                popup("没踩住…", lw / 2f, lh / 2f, 0xFFE8574F, 18);
                dirty();
                postDelayedSafe(() -> host.gameOver(score * 50), 800);
                return;
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF121A26);
        // 当前平台与目标平台
        rr(c, platX, GROUND - 90, platX + platW, GROUND - 76, 8, 0xFF3E5C9A);
        rr(c, 20, GROUND - 20, 100, GROUND, 8, 0xFF3E5C9A);
        // 蓄力条
        if (holding) {
            rr(c, 30, 80, 330, 96, 6, 0xFF2A2D37);
            rr(c, 30, 80, 30 + 300 * charge, 96, 6,
                    charge > 0.85f ? 0xFFE8574F : (charge > 0.5f ? 0xFFFFC24B : 0xFF7CFFB2));
        }
        // 果冻
        float squash = flying ? 0.8f : (holding ? 1f - charge * 0.25f : 1f);
        float jw = 26 / squash, jh = 26 * squash;
        fill.setColor(0xFF7CFFB2);
        rf.set(px - jw / 2f, py + 12 - jh, px + jw / 2f, py + 12);
        c.drawRoundRect(rf, 10, 10, fill);
        fill.setColor(0xFF14342B);
        c.drawCircle(px - 5, py + 12 - jh * 0.6f, 2.5f, fill);
        c.drawCircle(px + 5, py + 12 - jh * 0.6f, 2.5f, fill);
        label(c, "连跳 " + score, lw - 46, 40, 15, 0xFF9AA0AC, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
