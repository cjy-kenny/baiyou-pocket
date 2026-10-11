package com.baiyou.pocket.game;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 目标点杀家族基类：目标/炸弹在盘面随机冒出并限时消失，
 *  点中目标得分，点中炸弹或漏掉目标扣命，3 命尽结算（或限时回合）。 */
public abstract class PopTargetGame extends CanvasGame {
    public static final int KIND_TARGET = 0, KIND_BOMB = 1, KIND_BONUS = 2;

    protected final List<float[]> ents = new ArrayList<>(); // {x,y,r,born,life,kind}
    protected int score, lives = 3;
    private long lastSpawn;
    private boolean dying;

    protected PopTargetGame() {
        super(360, 560);
    }

    /** 两次生成之间的间隔 ms（可随难度收紧）。 */
    protected abstract long spawnGapMs();

    /** 生成一种实体（默认目标为主，子类掺炸弹/奖励）。 */
    protected int nextKind() {
        return KIND_TARGET;
    }

    /** 目标存活时长 ms。 */
    protected long lifeMs() {
        return 1100;
    }

    /** 漏掉目标是否扣命（打地鼠类不扣）。 */
    protected boolean missCostsLife() {
        return true;
    }

    /** 点中目标得分。 */
    protected int hitScore(int kind) {
        return kind == KIND_BONUS ? 30 : 10;
    }

    /** 同屏上限。 */
    protected int maxOnScreen() {
        return 4;
    }

    /** 命上限（漏敌/中雷扣除）。 */
    protected int maxLives() {
        return 3;
    }

    /** 回合时长 ms；0 = 无限时（靠命结束）。 */
    protected long roundMs() {
        return 60000;
    }

    /** 背景与氛围（子类覆写）。 */
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1B1E26);
    }

    /** 实体皮肤。 */
    protected void drawEnt(Canvas c, float[] e) {
        long age = now() - (long) e[3];
        float k = Math.min(1f, age / 140f); // 弹入
        float r = e[2] * (0.6f + 0.4f * easeOut(k));
        int kind = (int) e[5];
        int color = kind == KIND_BOMB ? 0xFF3A3F4C : (kind == KIND_BONUS ? 0xFFFFC24B : 0xFFE8574F);
        fill.setColor(color);
        c.drawCircle(e[0], e[1], r, fill);
        if (kind == KIND_BOMB) {
            label(c, "✸", e[0], e[1], r * 1.6f, 0xFFFFC24B, true);
        } else if (kind == KIND_BONUS) {
            label(c, "★", e[0], e[1], r * 1.5f, 0xFFFFFFFF, true);
        }
    }

    protected final void spawnEnt(float x, float y, float r, long life, int kind) {
        ents.add(new float[]{x, y, r, now(), life, kind, 0, 0}); // [6]/[7] 子类自定义
    }

    /** 生成一个实体（默认：kind=nextKind，随机位置）；子类可整体覆写。 */
    protected void spawnOne() {
        spawnEnt(rand(40, lw - 40), rand(110, lh - 90), rand(22, 30), lifeMs(), nextKind());
    }

    /** 刚生成的实体（spawnOne 覆写后用于填自定义数据）。 */
    protected final float[] lastEnt() {
        return ents.get(ents.size() - 1);
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying || action != 0) return;
        // 从上层往下找命中
        for (int i = ents.size() - 1; i >= 0; i--) {
            float[] e = ents.get(i);
            float dx = x - e[0], dy = y - e[1];
            if (dx * dx + dy * dy <= (e[2] + 6) * (e[2] + 6)) {
                ents.remove(i);
                int kind = (int) e[5];
                if (kind == KIND_BOMB) {
                    lives--;
                    beep(2);
                    shake(220);
                    burst(e[0], e[1], 0xFFE8574F, 14, 6f);
                    popup("是炸弹！", e[0], e[1] - 20, 0xFFE8574F, 15);
                } else {
                    int gain = hitScore(kind);
                    score += gain;
                    host.setScore(score);
                    beep(1);
                    burst(e[0], e[1], 0xFF7CFFB2, 10, 5f);
                    popup("+" + gain, e[0], e[1] - 20, 0xFF7CFFB2, 17);
                }
                if (lives <= 0) {
                    finish("生命用完了");
                }
                dirty();
                return;
            }
        }
    }

    @Override
    protected void onTick() {
        long now = now();
        // 生成
        if (ents.size() < maxOnScreen() && now - lastSpawn >= spawnGapMs()) {
            lastSpawn = now;
            spawnOne();
        }
        // 超时消失
        Iterator<float[]> it = ents.iterator();
        while (it.hasNext()) {
            float[] e = it.next();
            if (now - (long) e[3] > (long) e[4]) {
                it.remove();
                if ((int) e[5] != KIND_BOMB && missCostsLife()) {
                    lives--;
                    beep(2);
                    popup("漏掉了！", e[0], e[1] - 16, 0xFFE8574F, 14);
                    if (lives <= 0) {
                        finish("生命用完了");
                        return;
                    }
                }
            }
        }
        // 回合时间
        if (roundMs() > 0 && now - roundEnd() >= 0) {
            finish("时间到");
            return;
        }
        dirty();
    }

    private long roundEnd() {
        return sessionStart() + roundMs();
    }

    private long sessionStart;

    private long sessionStart() {
        return sessionStart;
    }

    private void finish(String why) {
        dying = true;
        over = true;
        stopLoop();
        ents.clear();
        popup(why + " 得分 " + score, lw / 2f, lh / 2f, 0xFFECECF1, 18);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 800);
    }

    @Override
    public void restartGame() {
        sessionStart = now();
        ents.clear();
        score = 0;
        lives = maxLives();
        lastSpawn = 0;
        dying = false;
        over = false;
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        drawBg(c);
        for (float[] e : ents) drawEnt(c, e);
        hearts(c, lives, maxLives(), lw - 24, 30, 8);
        if (roundMs() > 0) {
            float remain = Math.max(0f, 1f - (now() - sessionStart) / (float) roundMs());
            rr(c, 16, 56, lw - 16, 62, 4, 0xFF2A2D37);
            rr(c, 16, 56, 16 + (lw - 32) * remain, 62, 4, 0xFF7B7FF5);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
