package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g088 太空弹幕：飞船横移躲弹幕，同时点屏反击 boss；
 *  撞弹扣护盾，撑过 60 秒或击破 boss 均可获胜。 */
public class BulletHellGame extends CanvasGame {
    private static class Bullet {
        float x, y, vx, vy;

        Bullet(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    private final List<Bullet> bullets = new ArrayList<>();
    private final Random rnd = new Random();
    private float px = 180;
    private float bossX = 180, bossDir = 1;
    private int bossHp = 60;
    private int shield = 3, score;
    private long lastFire, startAt;
    private boolean dying;
    private boolean firing;

    public BulletHellGame() {
        super(360, 560);
    }

    private void startRound() {
        bullets.clear();
        px = 180;
        bossX = 180;
        bossHp = 60;
        shield = 3;
        score = 0;
        startAt = System.currentTimeMillis();
        lastFire = 0;
        dying = false;
        firing = false;
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
        px = Math.max(20, Math.min(lw - 20, x));
        firing = action == 0 || action == 2;
        if (action == 1) firing = false;
        dirty();
    }

    private void winRound(int bonus) {
        score += bonus;
        host.setScore(score);
        dying = true;
        over = true;
        stopLoop();
        popup(bonus > 0 ? "击破 Boss！ +500" : "撑过 60 秒！ +300", lw / 2f, lh / 2f, 0xFFFFC24B, 20);
        burst(bossX, 150, 0xFFFFC24B, 26, 9f);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 950);
    }

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        bossX += bossDir * 1.6f;
        if (bossX < 70 || bossX > lw - 70) bossDir = -bossDir;
        // boss 弹幕
        if (now - lastFire > Math.max(240, 520 - score / 12)) {
            lastFire = now;
            int n = bossHp < 25 ? 3 : 2;
            for (int i = 0; i < n; i++) {
                float dx = px - bossX + rand(-40, 40);
                float dy = 470 - 120;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                bullets.add(new Bullet(bossX, 130, dx / len * 3.6f, dy / len * 3.6f));
            }
            beep(0);
        }
        // 我方射击
        if (firing && now - lastFireAt > 130) {
            shots.add(new float[]{px, 460});
            lastFireAt = now;
        }
        // 我方子弹
        Iterator<float[]> sit = shots.iterator();
        while (sit.hasNext()) {
            float[] s = sit.next();
            s[1] -= 10;
            if (s[1] < 100) {
                sit.remove();
                continue;
            }
            float dx = s[0] - bossX, dy = s[1] - 120;
            if (dx * dx + dy * dy < 34 * 34) {
                sit.remove();
                bossHp--;
                score += 5;
                host.setScore(score);
                beep(0);
                burst(s[0], 120, 0xFF7CFFB2, 4, 3f);
                if (bossHp <= 0) {
                    winRound(500);
                    return;
                }
            }
        }
        // 敌弹
        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            b.x += b.vx;
            b.y += b.vy;
            if (b.y > lh + 10 || b.x < -10 || b.x > lw + 10) {
                bit.remove();
                continue;
            }
            float dx = b.x - px, dy = b.y - 470;
            if (dx * dx + dy * dy < 15 * 15) {
                bit.remove();
                shield--;
                beep(2);
                shake(220);
                if (shield <= 0) {
                    dying = true;
                    over = true;
                    stopLoop();
                    popup("被弹幕吞没", lw / 2f, lh / 2f, 0xFFE8574F, 19);
                    dirty();
                    postDelayedSafe(() -> host.gameOver(score), 850);
                    return;
                }
            }
        }
        if (now - startAt > 60000) {
            winRound(300);
            return;
        }
        dirty();
    }

    // 简易我方子弹列表
    private final List<float[]> shots = new ArrayList<>();
    private long lastFireAt;

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0B0E1A);
        // boss
        fill.setColor(0xFF9B59F6);
        rf.set(bossX - 44, 96, bossX + 44, 144);
        c.drawRoundRect(rf, 12, 12, fill);
        fill.setColor(0xFFE8848C);
        c.drawCircle(bossX - 18, 120, 7, fill);
        c.drawCircle(bossX + 18, 120, 7, fill);
        // 血条
        rr(c, 40, 70, lw - 40, 80, 4, 0xFF2A2D37);
        rr(c, 40, 70, 40 + (lw - 80) * Math.max(0, bossHp) / 60f, 80, 4, 0xFFE8574F);
        // 弹幕
        for (Bullet b : bullets) {
            fill.setColor(0xFFE8848C);
            c.drawCircle(b.x, b.y, 6, fill);
            fill.setColor(0xFFFFFFFF);
            c.drawCircle(b.x, b.y, 2.5f, fill);
        }
        // 我方子弹
        for (float[] s : shots) {
            fill.setColor(0xFF7CFFB2);
            rr(c, s[0] - 2, s[1] - 10, s[0] + 2, s[1] + 2, 2, 0xFF7CFFB2);
        }
        // 飞船
        Path p = new Path();
        p.moveTo(px, 452);
        p.lineTo(px - 18, 486);
        p.lineTo(px + 18, 486);
        p.close();
        fill.setColor(0xFF7CFFB2);
        c.drawPath(p, fill);
        hearts(c, shield, 3, lw - 24, 34, 8);
        label(c, "拖动躲弹 · 按住反击", lw / 2f, 40, 12, 0xFF8B93A7, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
