package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g084 火箭拦截：来袭飞弹拖着尾焰下坠，点它拦截（拦截圈略大）；
 *  落地扣护盾，护盾尽则失守。 */
public class RocketInterceptGame extends CanvasGame {
    private static class Missile {
        float x, y, vy;
        boolean dead;
        long born;

        Missile(float x, float y, float vy) {
            this.x = x;
            this.y = y;
            this.vy = vy;
            this.born = System.currentTimeMillis();
        }
    }

    private final List<Missile> missiles = new ArrayList<>();
    private final Random rnd = new Random();
    private int shield = 3, score;
    private long lastSpawn;
    private boolean dying;

    public RocketInterceptGame() {
        super(360, 560);
    }

    private void startRound() {
        missiles.clear();
        shield = 3;
        score = 0;
        lastSpawn = 0;
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
        for (Missile m : missiles) {
            if (m.dead) continue;
            float dx = x - m.x, dy = y - m.y;
            if (dx * dx + dy * dy < 44 * 44) {
                m.dead = true;
                score += 25;
                host.setScore(score);
                beep(1);
                burst(m.x, m.y, 0xFFFFC24B, 16, 6f);
                popup("+25", m.x, m.y - 24, 0xFFFFC24B, 16);
                return;
            }
        }
        beep(2);
    }

    private void breach(float x) {
        shield--;
        beep(2);
        shake(260);
        burst(x, 520, 0xFFE8574F, 18, 7f);
        if (shield <= 0) {
            dying = true;
            over = true;
            stopLoop();
            popup("防线失守", lw / 2f, lh / 2f, 0xFFE8574F, 19);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 850);
        }
        dirty();
    }

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        if (now - lastSpawn > Math.max(500, 1200 - score * 8)) {
            lastSpawn = now;
            missiles.add(new Missile(rand(40, lw - 40), -20, rand(3.2f, 5.2f)));
        }
        Iterator<Missile> it = missiles.iterator();
        while (it.hasNext()) {
            Missile m = it.next();
            if (m.dead) {
                it.remove();
                continue;
            }
            m.y += m.vy;
            m.x += (float) Math.sin((now - m.born) / 400.0) * 1.2f;
            if (m.y > 520) {
                it.remove();
                breach(m.x);
                if (over) return;
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0E1420);
        // 城市
        fill.setColor(0xFF1E2A3E);
        for (int i = 0; i < 7; i++) {
            float bw = 40 + (i % 3) * 14;
            rr(c, 10 + i * 50, 545 - (30 + (i * 29) % 40), 10 + i * 50 + bw, 545, 3, 0xFF1E2A3E);
        }
        hearts(c, shield, 3, lw - 24, 30, 8);
        label(c, "拦截 " + score / 25 + " 枚", 40, 30, 14, 0xFF9AA0AC, true);
        for (Missile m : missiles) {
            if (m.dead) continue;
            // 尾焰
            stroke.setColor(0xFFFF9D5C);
            stroke.setStrokeWidth(3);
            c.drawLine(m.x, m.y, m.x - 3, m.y - 26, stroke);
            fill.setColor(0xFFE8574F);
            Path p = new Path();
            p.moveTo(m.x, m.y + 14);
            p.lineTo(m.x - 9, m.y - 10);
            p.lineTo(m.x + 9, m.y - 10);
            p.close();
            c.drawPath(p, fill);
            fill.setColor(0xFF7CFFB2);
            c.drawCircle(m.x, m.y - 2, 3.5f, fill);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
