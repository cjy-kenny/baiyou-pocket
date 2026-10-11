package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g035 弹跳小虾：点按左右半屏控制横向，弹簧床自动起跳往上蹦，
 *  踩空坠落即结束，跳得越高分越高。 */
public class BouncyUpGame extends CanvasGame {
    private static class Plat {
        float x, y, w;

        Plat(float x, float y, float w) {
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }

    private final List<Plat> plats = new ArrayList<>();
    private final Random rnd = new Random();
    private float px, py, vx, vy;
    private int score, maxY;
    private float cam;
    private boolean dying;
    private boolean moveL, moveR;

    public BouncyUpGame() {
        super(360, 560);
        genStart();
    }

    private void genStart() {
        plats.clear();
        plats.add(new Plat(lw / 2f - 45, 500, 90));
        float y = 500;
        while (y > -800) {
            y -= rand(60, 95);
            plats.add(new Plat(rand(15, lw - 95), y, rand(70, 105)));
        }
        px = lw / 2f;
        py = 470;
        vx = vy = 0;
        cam = 0;
        maxY = 0;
    }

    private void startRound() {
        genStart();
        score = 0;
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
        if (over || dying) return;
        moveL = action == 0 || action == 2 ? x < lw / 2f : false;
        moveR = action == 0 || action == 2 ? x >= lw / 2f : false;
        if (action == 1) {
            moveL = moveR = false;
        }
    }

    @Override
    protected void onTick() {
        if (moveL) vx = Math.max(-6f, vx - 0.7f);
        else if (moveR) vx = Math.min(6f, vx + 0.7f);
        else vx *= 0.85f;
        px = Math.max(18, Math.min(lw - 18, px + vx));

        vy += 0.34f;
        py += vy;

        // 平台碰撞（下落时）
        if (vy > 0) {
            for (Plat p : plats) {
                if (px > p.x - 8 && px < p.x + p.w + 8
                        && py + 14 > p.y && py + 14 < p.y + 18 + vy) {
                    vy = -12.4f;
                    beep(0);
                    burst(px, p.y, 0xFF7CFFB2, 6, 3f);
                    break;
                }
            }
        }
        // 镜头跟随
        float targetCam = py - 300;
        if (targetCam > cam) cam = targetCam;
        // 出屏平台清理 & 上方补台
        Iterator<Plat> it = plats.iterator();
        while (it.hasNext()) if (it.next().y > cam + lh + 40) it.remove();
        float top = Integer.MIN_VALUE;
        for (Plat p : plats) top = Math.max(top, p.y);
        while (top > cam - 80) {
            top -= rand(60, 95);
            plats.add(new Plat(rand(15, lw - 95), top, rand(70, 105)));
        }
        // 计分：高度
        int h = Math.max(0, (int) ((470 - py - cam) / 6));
        if (h > maxY) {
            score += h - maxY;
            maxY = h;
            host.setScore(score);
        }
        // 坠落
        if (py - cam > lh + 40) {
            dying = true;
            over = true;
            stopLoop();
            beep(2);
            popup("踩空了…", lw / 2f, lh / 2f, 0xFFE8574F, 18);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF101826);
        // 平台（世界坐标 → 屏幕）
        for (Plat p : plats) {
            float sy = p.y - cam;
            rr(c, p.x, sy, p.x + p.w, sy + 14, 7, 0xFF3E5C9A);
            rr(c, p.x + 4, sy + 3, p.x + p.w - 4, sy + 8, 3, 0xFF5B8DEF);
        }
        // 小虾
        float sxp = px, syp = py - cam;
        fill.setColor(0xFFFF9D5C);
        c.drawCircle(sxp, syp, 14, fill);
        stroke.setColor(0xFF1B1C22);
        stroke.setStrokeWidth(2);
        c.drawLine(sxp + 10, syp - 4, sxp + 18, syp - 10, stroke);
        fill.setColor(0xFF1B1C22);
        c.drawCircle(sxp + 5, syp - 4, 2.5f, fill);
        label(c, score + " m", lw - 46, 30, 16, 0xFF9AA0AC, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
