package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g036 星尘阻击：敌机成排压境，点屏开火（从炮位直上），
 *  清空一波升级下一波，敌机压到底线即结束。 */
public class InvadersGame extends CanvasGame {
    private static final int COLS = 6;

    private static class Enemy {
        float x, y;
        int hp;
        float drift;

        Enemy(float x, float y, int hp) {
            this.x = x;
            this.y = y;
            this.hp = hp;
        }
    }

    private static class Shot {
        float x, y;

        Shot(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();
    private final Random rnd = new Random();
    private int wave, score, lives = 3;
    private float shipX = 180;
    private float driftDir = 1;
    private boolean dying;

    public InvadersGame() {
        super(360, 560);
        buildWave();
    }

    private void buildWave() {
        enemies.clear();
        shots.clear();
        int rows = Math.min(4, 2 + wave / 2);
        int hp = 1 + wave / 3;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < COLS; c++)
                enemies.add(new Enemy(45 + c * 54, 110 + r * 46, hp));
        driftDir = rnd.nextBoolean() ? 1 : -1;
    }

    private void startRound() {
        wave = 1;
        score = 0;
        lives = 3;
        buildWave();
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
        shipX = Math.max(30, Math.min(lw - 30, x));
        if (action == 0) {
            shots.add(new Shot(shipX, 500));
            beep(0);
        }
        dirty();
    }

    private void die() {
        dying = true;
        over = true;
        stopLoop();
        shake(320);
        burst(shipX, 510, 0xFFE8574F, 20, 7f);
        beep(2);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 800);
    }

    @Override
    protected void onTick() {
        // 敌机漂移 + 下压
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (Enemy e : enemies) {
            e.x += driftDir * (0.4f + wave * 0.08f);
            minX = Math.min(minX, e.x);
            maxX = Math.max(maxX, e.x);
        }
        if (maxX > lw - 30) driftDir = -1;
        if (minX < 30) driftDir = 1;
        for (Enemy e : enemies) e.y += 0.06f + wave * 0.02f;
        // 子弹
        Iterator<Shot> sit = shots.iterator();
        while (sit.hasNext()) {
            Shot s = sit.next();
            s.y -= 9;
            if (s.y < 60) {
                sit.remove();
                continue;
            }
            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy e = eit.next();
                float dx = s.x - e.x, dy = s.y - e.y;
                if (dx * dx + dy * dy < 22 * 22) {
                    e.hp--;
                    sit.remove();
                    if (e.hp <= 0) {
                        eit.remove();
                        score += 20;
                        host.setScore(score);
                        beep(1);
                        burst(e.x, e.y, 0xFFE8848C, 10, 5f);
                    } else {
                        beep(0);
                    }
                    break;
                }
            }
        }
        // 敌机触底
        for (Enemy e : enemies) {
            if (e.y > 490) {
                lives--;
                e.y = -40; // 送回顶部
                beep(2);
                shake(200);
                if (lives <= 0) {
                    die();
                    return;
                }
            }
        }
        if (enemies.isEmpty()) {
            wave++;
            score += 100;
            host.setScore(score);
            popup("第 " + wave + " 波来袭！", lw / 2f, lh / 2f - 40, 0xFFFFC24B, 18);
            buildWave();
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0C0F1A);
        stroke.setColor(0xFF3A4266);
        stroke.setStrokeWidth(1);
        c.drawLine(0, 505, lw, 505, stroke);
        for (Enemy e : enemies) {
            fill.setColor(e.hp > 1 ? 0xFF9B59F6 : 0xFFE8848C);
            rr(c, e.x - 18, e.y - 12, e.x + 18, e.y + 12, 5, fill.getColor());
            fill.setColor(0xFF101318);
            c.drawCircle(e.x - 7, e.y, 3, fill);
            c.drawCircle(e.x + 7, e.y, 3, fill);
        }
        for (Shot s : shots) {
            fill.setColor(0xFFFFC24B);
            rr(c, s.x - 2, s.y - 10, s.x + 2, s.y + 4, 2, 0xFFFFC24B);
        }
        // 炮位
        fill.setColor(0xFF7CFFB2);
        rr(c, shipX - 20, 505, shipX + 20, 525, 5, 0xFF7CFFB2);
        Path p = new Path();
        p.moveTo(shipX, 480);
        p.lineTo(shipX - 16, 510);
        p.lineTo(shipX + 16, 510);
        p.close();
        fill.setColor(0xFF7CFFB2);
        c.drawPath(p, fill);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "第 " + wave + " 波", 44, 30, 14, 0xFF9AA0AC, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
