package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g041 藤蔓飞跃：抓住摆荡的藤蔓，松手把身体抛出去，再抓下一根；
 *  抓得越远分越高，坠落即结束。 */
public class VineSwingGame extends CanvasGame {
    private static final float GROUND = 520;

    private final Random rnd = new Random();
    // 藤蔓锚点
    private float vineX = 180, vineLen = 210;
    private float swing;        // 摆角
    private float swingV = 2.2f;
    private boolean hanging = true;
    private float px, py, vx, vy;
    private int score;
    private boolean dying;

    public VineSwingGame() {
        super(360, 560);
    }

    private void newVine(float fromX) {
        vineX = Math.min(lw - 60, Math.max(60, fromX + rand(110, 160)));
        vineLen = rand(170, 220);
        swing = rand(-0.5f, 0.5f);
        swingV = rand(1.8f, 2.6f) * (swing > 0 ? -1 : 1);
        hanging = true;
    }

    private void startRound() {
        px = 180;
        py = 200;
        score = 0;
        newVine(150);
        dying = false;
        over = false;
        host.setScore(0);
        startLoop(16);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying || action != 0) return;
        if (hanging) {
            // 松手：沿切线方向抛出
            float tanX = (float) Math.cos(swing) * swingV * vineLen;
            float tanY = (float) Math.sin(swing) * swingV * vineLen;
            vx = -tanX * 0.9f;
            vy = tanY * 0.9f - 2f;
            hanging = false;
            beep(0);
        } else {
            // 尝试抓藤蔓：抓住判定圈画出来，降低挫败感
            float hx = vineX + (float) Math.sin(swing) * vineLen;
            float hy = 60 + (float) Math.cos(swing) * vineLen;
            float dx = x - hx, dy = y - hy;
            if (dx * dx + dy * dy < 150 * 150) {
                hanging = true;
                score++;
                host.setScore(score * 100);
                beep(1);
                burst(hx, hy, 0xFF9ACDAA, 10, 5f);
                popup("+ " + score * 100, hx, hy - 20, 0xFF9ACDAA, 15);
                newVine(px);
            } else {
                beep(2);
                popup("差一点！", x, y - 16, 0xFFE8574F, 14);
            }
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (hanging) {
            // 单摆
            float g = 0.055f;
            swingV += -g * (float) Math.sin(swing) * 12;
            swingV *= 1.0005f;
            swing += swingV / 60f;
            px = vineX + (float) Math.sin(swing) * vineLen;
            py = 60 + (float) Math.cos(swing) * vineLen;
        } else {
            vy += 0.3f;
            px += vx;
            py += vy;
            if (px > lw + 40 || px < -40) {
                px = Math.max(60, Math.min(lw - 60, px));
                newVine(px);
                hanging = true;
                score = Math.max(0, score); // 抓不回原藤蔓不扣分
            }
            if (py > GROUND) {
                dying = true;
                over = true;
                stopLoop();
                shake(300);
                burst(px, GROUND, 0xFFE8574F, 16, 6f);
                beep(2);
                popup("摔下来了", lw / 2f, lh / 2f, 0xFFE8574F, 18);
                dirty();
                postDelayedSafe(() -> host.gameOver(score * 100), 800);
                return;
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF141E1A);
        // 丛林剪影
        fill.setColor(0xFF1B2A22);
        for (int i = 0; i < 7; i++) {
            float tx = i * 60 + ((i * 37) % 23);
            c.drawCircle(tx, GROUND - 30, 34, fill);
            rr(c, tx - 4, GROUND - 30, tx + 4, GROUND, 3, 0xFF1B2A22);
        }
        rr(c, 0, GROUND, lw, lh - GROUND, 0, 0xFF22301F);
        // 藤蔓
        float hx = vineX + (float) Math.sin(swing) * vineLen;
        float hy = 60 + (float) Math.cos(swing) * vineLen;
        stroke.setColor(0xFFB9D98A);
        stroke.setStrokeWidth(5);
        c.drawLine(vineX, 0, hanging ? hx : px, hanging ? hy : py, stroke);
        fill.setColor(0xFF5A7030);
        c.drawCircle(vineX, 0, 10, fill);
        if (!hanging) {
            // 可抓范围提示
            stroke.setColor(0x449ACDAA);
            stroke.setStrokeWidth(2);
            c.drawCircle(hx, hy, 150, stroke);
        }
        // 角色
        fill.setColor(0xFFE8A87C);
        c.drawCircle(hanging ? hx : px, (hanging ? hy : py) + 14, 11, fill);
        rr(c, (hanging ? hx : px) - 9, (hanging ? hy : py) + 24, (hanging ? hx : px) + 9,
                (hanging ? hy : py) + 40, 4, 0xFF2FA36B);
        label(c, "点屏：松手 / 抓藤", lw / 2f, 36, 13, 0xFF9ACDAA, false);
        label(c, "已越 " + score + " 藤", lw - 50, 60, 14, 0xFFECECF1, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
