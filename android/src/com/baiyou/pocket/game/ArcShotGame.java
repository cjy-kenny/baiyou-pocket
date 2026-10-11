package com.baiyou.pocket.game;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 抛射瞄准家族基类：从发射点向后拖拽蓄力（弹弓式），松手发射，
 *  受重力抛物线飞行，命中目标得分；每轮限定弹数，清空目标或弹尽结算。 */
public abstract class ArcShotGame extends CanvasGame {
    protected final List<float[]> targets = new ArrayList<>(); // {x,y,r,kind,vx}
    protected float launcherX = 60, launcherY = 470;
    protected float projX, projY, projVx, projVy;
    private boolean flying;
    private float dragX = -1, dragY = -1;
    protected int score, shotsLeft, round;
    private boolean settling;

    protected ArcShotGame() {
        super(360, 560);
    }

    /** 重力加速度（逻辑 px/tick²）。 */
    protected float gravity() {
        return 0.14f;
    }

    /** 力度放大系数：拖拽距离 → 初速。 */
    protected float powerScale() {
        return 0.09f;
    }

    /** 每轮弹数。 */
    protected int shotsPerRound() {
        return 5;
    }

    /** 出题：布置 targets（round 从 1 开始）。 */
    protected abstract void buildTargets(int round);

    /** 目标总轮数（打完进入下一轮，超过则胜利结算）。 */
    protected abstract int totalRounds();

    /** 命中目标得分。 */
    protected int hitScore(float[] t) {
        return 100;
    }

    /** 目标绘制。 */
    protected void drawTarget(Canvas c, float[] t) {
        fill.setColor(0xFFE8574F);
        c.drawCircle(t[0], t[1], t[2], fill);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(t[0], t[1], t[2] * 0.45f, fill);
    }

    /** 背景（子类可覆写）。 */
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1B222C);
        rr(c, 0, 520, lw, lh, 0, 0xFF242B36); // 地面
    }

    /** 飞行物出界（落地/出边）回调：默认弹数-1。 */
    protected void onShotMissed() {
    }

    /** 发射器装饰（弓等，子类可选）。 */
    protected void drawLauncherDecor(Canvas c) {
    }

    @Override
    public void restartGame() {
        score = 0;
        round = 1;
        shotsLeft = shotsPerRound();
        targets.clear();
        flying = false;
        settling = false;
        over = false;
        host.setScore(0);
        buildTargets(round);
        startLoop(16);
        dirty();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling) return;
        if (action == 0 && !flying && hit(x, y, launcherX - 60, launcherY - 60, 120, 120)) {
            dragX = x;
            dragY = y;
        } else if (action == 2 && dragX >= 0) {
            dragX = x;
            dragY = y;
        } else if (action == 1 && dragX >= 0) {
            float dx = launcherX - dragX, dy = launcherY - dragY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 24 && shotsLeft > 0) {
                projX = launcherX;
                projY = launcherY;
                projVx = dx * powerScale();
                projVy = dy * powerScale();
                flying = true;
                shotsLeft--;
                beep(0);
            }
            dragX = dragY = -1;
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (flying) {
            projVy += gravity();
            projX += projVx;
            projY += projVy;
            // 命中检测
            for (Iterator<float[]> it = targets.iterator(); it.hasNext(); ) {
                float[] t = it.next();
                float dx = projX - t[0], dy = projY - t[1];
                if (dx * dx + dy * dy <= t[2] * t[2]) {
                    it.remove();
                    int gain = hitScore(t);
                    score += gain;
                    host.setScore(score);
                    beep(1);
                    burst(t[0], t[1], 0xFFFFC24B, 14, 6f);
                    popup("+" + gain, t[0], t[1] - 22, 0xFFFFC24B, 18);
                    flying = false;
                    break;
                }
            }
            // 目标自身移动
            for (float[] t : targets) {
                t[0] += t[3];
                if (t[0] < t[2] || t[0] > lw - t[2]) t[3] = -t[3];
            }
            // 出界
            if (flying && (projX < -30 || projX > lw + 30 || projY > 540)) {
                flying = false;
                onShotMissed();
                beep(2);
            }
        }
        // 轮次推进
        if (!flying && targets.isEmpty() && !settling) {
            settling = true;
            postDelayedSafe(this::advanceRound, 700);
        }
        if (!flying && shotsLeft <= 0 && !targets.isEmpty() && !settling) {
            settling = true;
            postDelayedSafe(this::finishShots, 700);
        }
        dirty();
    }

    private void advanceRound() {
        settling = false;
        round++;
        if (round > totalRounds()) {
            over = true;
            stopLoop();
            popup("全部命中！", lw / 2f, lh / 2f, 0xFFFFC24B, 24);
            beep(3);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 850);
            return;
        }
        shotsLeft = shotsPerRound();
        buildTargets(round);
        popup("第 " + round + " 轮", lw / 2f, lh / 2f - 40, 0xFFECECF1, 18);
        dirty();
    }

    private void finishShots() {
        settling = false;
        over = true;
        stopLoop();
        popup("弹药用完了", lw / 2f, lh / 2f, 0xFFECECF1, 18);
        beep(2);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 850);
    }

    @Override
    protected void render(Canvas c) {
        drawBg(c);
        // 瞄准线（拖拽时显示反向预测）
        if (dragX >= 0 && !flying) {
            float dx = launcherX - dragX, dy = launcherY - dragY;
            float len = (float) Math.max(24, Math.sqrt(dx * dx + dy * dy));
            float ux = dx / len, uy = dy / len;
            stroke.setColor(0x88FFFFFF);
            stroke.setStrokeWidth(3);
            float sx = launcherX, sy = launcherY, svx = ux * len * powerScale(), svy = uy * len * powerScale();
            float px2 = sx, py2 = sy;
            for (int i = 0; i < 26; i++) {
                svy += gravity();
                px2 += svx;
                py2 += svy;
                c.drawCircle(px2, py2, 2.2f, stroke);
            }
        }
        for (float[] t : targets) drawTarget(c, t);
        // 发射器
        fill.setColor(0xFF5B8DEF);
        c.drawCircle(launcherX, launcherY, 16, fill);
        rr(c, launcherX - 12, launcherY + 12, launcherX + 12, launcherY + 30, 4, 0xFF3E4B66);
        drawLauncherDecor(c);
        // 飞行物
        if (flying) {
            fill.setColor(0xFFF4F5F9);
            c.drawCircle(projX, projY, 7, fill);
        }
        label(c, "剩余弹药 " + shotsLeft, lw - 56, 26, 13, 0xFF9AA0AC, true);
        label(c, "第 " + Math.min(round, totalRounds()) + "/" + totalRounds() + " 轮", lw - 56, 48, 12, 0xFF6B7280, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
