package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g086 深海鱼雷：声呐回波标出敌艇方位，点回波发射鱼雷；
 *  敌艇逃出屏幕扣护盾，护盾尽结算。 */
public class TorpedoGame extends CanvasGame {
    private static class Sub {
        float x, y, vx;
        boolean dead;

        Sub(float x, float y, float vx) {
            this.x = x;
            this.y = y;
            this.vx = vx;
        }
    }

    private final List<Sub> subs = new ArrayList<>();
    private final List<float[]> torps = new ArrayList<>();
    private final Random rnd = new Random();
    private int shield = 3, score;
    private long lastSpawn;
    private boolean dying;

    public TorpedoGame() {
        super(360, 560);
    }

    private void startRound() {
        subs.clear();
        torps.clear();
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
        torps.add(new float[]{180, 520, (x - 180) / 60f, -7f});
        beep(0);
        dirty();
    }

    private void breach() {
        shield--;
        beep(2);
        shake(240);
        if (shield <= 0) {
            dying = true;
            over = true;
            stopLoop();
            popup("声呐失灵", lw / 2f, lh / 2f, 0xFFE8574F, 19);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 850);
        }
        dirty();
    }

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        if (subs.size() < 3 && now - lastSpawn > Math.max(700, 1500 - score * 10)) {
            lastSpawn = now;
            float from = rnd.nextBoolean() ? -30 : lw + 30;
            subs.add(new Sub(from, rand(160, 400), (from < 0 ? 1 : -1) * rand(1.4f, 2.6f)));
        }
        Iterator<Sub> sit = subs.iterator();
        while (sit.hasNext()) {
            Sub s = sit.next();
            if (s.dead) {
                sit.remove();
                continue;
            }
            s.x += s.vx;
            if (s.x < -50 || s.x > lw + 50) {
                sit.remove();
                breach();
                if (over) return;
            }
        }
        Iterator<float[]> tit = torps.iterator();
        while (tit.hasNext()) {
            float[] t = tit.next();
            t[2] *= 1.005f;
            t[0] += t[2];
            t[1] += t[3];
            if (t[1] < 80 || t[0] < -10 || t[0] > lw + 10) {
                tit.remove();
                continue;
            }
            for (Sub s : subs) {
                if (s.dead) continue;
                float dx = t[0] - s.x, dy = t[1] - s.y;
                if (dx * dx + dy * dy < 30 * 30) {
                    s.dead = true;
                    tit.remove();
                    score += 60;
                    host.setScore(score);
                    beep(1);
                    burst(s.x, s.y, 0xFF7CFFB2, 14, 6f);
                    popup("+60", s.x, s.y - 24, 0xFF7CFFB2, 16);
                    break;
                }
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0A1A2A);
        // 深海渐层
        fill.setColor(0xFF0D2238);
        rr(c, 0, 200, lw, lh - 200, 0, 0xFF0D2238);
        // 声呐网格
        stroke.setColor(0x222E5C8A);
        stroke.setStrokeWidth(1);
        for (int i = 0; i < 5; i++) {
            c.drawCircle(180, 520, 60 + i * 90, stroke);
        }
        hearts(c, shield, 3, lw - 24, 30, 8);
        label(c, "击沉 " + score / 60 + " 艘", 40, 30, 14, 0xFF5C8ACD, true);
        // 敌艇
        for (Sub s : subs) {
            if (s.dead) continue;
            fill.setColor(0xFF3E5C66);
            rf.set(s.x - 26, s.y - 10, s.x + 26, s.y + 10);
            c.drawRoundRect(rf, 10, 10, fill);
            fill.setColor(0xFF7CFFB2);
            c.drawCircle(s.x + (s.vx > 0 ? -18 : 18), s.y, 3, fill);
        }
        // 鱼雷
        for (float[] t : torps) {
            fill.setColor(0xFFFFC24B);
            c.drawCircle(t[0], t[1], 6, fill);
        }
        // 发射台
        fill.setColor(0xFF3E4B66);
        rr(c, 150, 505, 210, 540, 6, 0xFF3E4B66);
        label(c, "点屏发鱼雷", lw / 2f, 480, 12, 0xFF5C8ACD, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
