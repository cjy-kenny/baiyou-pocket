package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g087 弓箭守城：城墙上拉弓放箭（拖拽瞄准），射退一波波步兵；
 *  步兵攻上城墙扣护盾，护盾尽则城破。 */
public class BowDefenseGame extends CanvasGame {
    private static class Foe {
        float x, y;
        int hp;

        Foe(float x, float y, int hp) {
            this.x = x;
            this.y = y;
            this.hp = hp;
        }
    }

    private final List<Foe> foes = new ArrayList<>();
    private final List<float[]> arrows = new ArrayList<>(); // {x,y,vx,vy}
    private final Random rnd = new Random();
    private int shield = 3, score, wave;
    private long lastSpawn;
    private float aimX = -1, aimY = -1;
    private boolean dying;

    public BowDefenseGame() {
        super(360, 560);
    }

    private void startRound() {
        foes.clear();
        arrows.clear();
        shield = 3;
        score = 0;
        wave = 1;
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
        if (over || dying) return;
        if (action == 0 || action == 2) {
            aimX = x;
            aimY = y;
        } else if (action == 1 && aimX >= 0) {
            float dx = aimX - 60, dy = aimY - 470;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 20) {
                float p = Math.min(12f, len * 0.075f);
                arrows.add(new float[]{60, 470, dx / len * p, dy / len * p});
                beep(0);
            }
            aimX = aimY = -1;
        }
        dirty();
    }

    private void breach() {
        shield--;
        beep(2);
        shake(260);
        if (shield <= 0) {
            dying = true;
            over = true;
            stopLoop();
            popup("城破", lw / 2f, lh / 2f, 0xFFE8574F, 20);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 850);
        }
        dirty();
    }

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        if (foes.size() < 4 && now - lastSpawn > Math.max(650, 1400 - score * 6)) {
            lastSpawn = now;
            int hp = 1 + wave / 3;
            foes.add(new Foe(lw + 20, 470, hp));
        }
        // 步兵推进
        Iterator<Foe> fit = foes.iterator();
        while (fit.hasNext()) {
            Foe f = fit.next();
            f.x -= 0.9f + wave * 0.12f;
            if (f.x < 90) {
                fit.remove();
                breach();
                if (over) return;
            }
        }
        // 箭
        Iterator<float[]> ait = arrows.iterator();
        while (ait.hasNext()) {
            float[] a = ait.next();
            a[3] += 0.09f;
            a[0] += a[2] * 2;
            a[1] += a[3] * 2;
            if (a[0] > lw + 20 || a[1] > 540) {
                ait.remove();
                continue;
            }
            for (Foe f : foes) {
                float dx = a[0] - f.x, dy = a[1] - f.y;
                if (dx * dx + dy * dy < 26 * 26) {
                    ait.remove();
                    f.hp--;
                    if (f.hp <= 0) {
                        fit.remove();
                        score += 30 * wave;
                        host.setScore(score);
                        beep(1);
                        burst(f.x, f.y, 0xFFE8848C, 12, 5f);
                    } else {
                        beep(0);
                    }
                    break;
                }
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E2620);
        // 地面
        rr(c, 0, 490, lw, lh - 490, 0, 0xFF2E3A26);
        // 城墙
        fill.setColor(0xFF6B6257);
        rr(c, 0, 460, 80, 560, 4, 0xFF6B6257);
        fill.setColor(0xFF57503F);
        rr(c, 10, 430, 70, 462, 3, 0xFF57503F);
        hearts(c, shield, 3, lw - 24, 30, 8);
        label(c, "第 " + wave + " 波", 40, 30, 14, 0xFF9ACDAA, true);
        // 弓箭手
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(60, 470, 14, fill);
        // 步兵
        for (Foe f : foes) {
            fill.setColor(f.hp > 1 ? 0xFF9B59F6 : 0xFFE8848C);
            rr(c, f.x - 10, f.y - 26, f.x + 10, f.y, 4, f.hp > 1 ? 0xFF9B59F6 : 0xFFE8848C);
            fill.setColor(0xFFE8A87C);
            c.drawCircle(f.x, f.y - 32, 7, fill);
        }
        // 箭
        for (float[] a : arrows) {
            stroke.setColor(0xFFF4F5F9);
            stroke.setStrokeWidth(2.5f);
            c.drawLine(a[0], a[1], a[0] - a[2] * 3, a[1] - a[3] * 3, stroke);
        }
        // 瞄准线
        if (aimX >= 0) {
            stroke.setColor(0x88FFC24B);
            stroke.setStrokeWidth(2);
            c.drawLine(60, 470, aimX, aimY, stroke);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
