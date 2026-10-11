package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g071 点球大战：两段式锁定——先停力度条，再停方向指针，门将对角扑救；
 *  五轮点球，进得越多分越高。 */
public class PenaltyKickGame extends CanvasGame {
    private static final float GOAL_L = 70, GOAL_R = 290, GOAL_TOP = 150, GOAL_BOT = 300;

    private final Random rnd = new Random();
    private int phase;      // 0=力度 1=方向 2=结果展示
    private float barT;     // 力度条进度 0..1 往返
    private float dirT;     // 方向指针 0..1 往返
    private int power, dir; // 锁定值 0..100 / 0..100（0=左 100=右）
    private int kick, goals;
    private long phaseUntil;
    private boolean settling;

    public PenaltyKickGame() {
        super(360, 560);
        startRound();
    }

    private void startRound() {
        kick = 0;
        goals = 0;
        phase = 0;
        barT = 0;
        dirT = 0;
        settling = false;
        over = false;
        startLoop(16);
        dirty();
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

    private void shoot() {
        // 门将随机扑向一边（带一点预判）
        int keeperDir = rnd.nextInt(100);
        // 球落点：dir 映射到门内横向位置
        float ballX = GOAL_L + (GOAL_R - GOAL_L) * dir / 100f;
        boolean high = power > 55;
        float ballY = high ? GOAL_TOP + 40 : GOAL_BOT - 40;
        float keeperX = GOAL_L + (GOAL_R - GOAL_L) * keeperDir / 100f;
        boolean out = dir < 4 || dir > 96; // 太偏打飞
        boolean saved = !out && Math.abs(keeperX - ballX) < 52 && !(high && power > 85);
        kick++;
        if (!out && !saved) {
            goals++;
            host.setScore(goals * 100);
            beep(3);
            popup("球进了！ +100", 180, 340, 0xFF7CFFB2, 20);
            burst(ballX, ballY, 0xFF7CFFB2, 14, 6f);
        } else {
            beep(2);
            popup(out ? "打飞了！" : "被扑出！", 180, 340, 0xFFE8574F, 18);
            shake(160);
        }
        phase = 2;
        phaseUntil = now() + 1100;
        dirty();
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        if (phase == 0) {
            power = (int) (barT * 100);
            phase = 1;
            beep(0);
        } else if (phase == 1) {
            dir = (int) (dirT * 100);
            shoot();
        }
        dirty();
    }

    @Override
    protected void onTick() {
        long now = now();
        if (phase == 0) {
            barT += 0.026f;
            if (barT > 1) barT = 0;
        } else if (phase == 1) {
            dirT += 0.02f;
            if (dirT > 1) dirT = 0;
        } else if (phase == 2 && now >= phaseUntil) {
            if (kick >= 5) {
                over = true;
                stopLoop();
                beep(3);
                popup("五轮 " + goals + " 中", lw / 2f, lh / 2f, 0xFFECECF1, 18);
                dirty();
                postDelayedSafe(() -> host.gameOver(goals * 100), 850);
                return;
            }
            phase = 0;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E3320);
        // 球门
        stroke.setColor(0xFFECECF1);
        stroke.setStrokeWidth(4);
        c.drawLine(GOAL_L, GOAL_BOT, GOAL_L, GOAL_TOP, stroke);
        c.drawLine(GOAL_R, GOAL_BOT, GOAL_R, GOAL_TOP, stroke);
        c.drawLine(GOAL_L, GOAL_TOP, GOAL_R, GOAL_TOP, stroke);
        fill.setColor(0x33202A20);
        rf.set(GOAL_L, GOAL_TOP, GOAL_R, GOAL_BOT);
        c.drawRoundRect(rf, 4, 4, fill);
        // 门将
        float keeperX = phase == 2 ? keeperShownX() : 180;
        fill.setColor(0xFFFFC24B);
        rr(c, keeperX - 20, GOAL_BOT - 60, keeperX + 20, GOAL_BOT, 8, 0xFFFFC24B);
        // 球
        fill.setColor(0xFFF4F5F9);
        c.drawCircle(180, 430, 16, fill);
        label(c, "第 " + (kick + 1) + "/5 轮 · 进 " + goals + " 球", 50, 30, 14, 0xFFECECF1, true);
        if (phase == 0) {
            label(c, "点屏锁力度", lw / 2f, 490, 16, 0xFFECECF1, true);
            rr(c, 40, 510, lw - 40, 530, 5, 0xFF2A3A2E);
            rr(c, 40, 510, 40 + (lw - 80) * barT, 530, 5, 0xFF7CFFB2);
        } else if (phase == 1) {
            label(c, "点屏锁方向", lw / 2f, 490, 16, 0xFFECECF1, true);
            rr(c, 40, 510, lw - 40, 530, 5, 0xFF2A3A2E);
            rr(c, 40, 510, 40 + (lw - 80) * dirT, 530, 5, 0xFFFFC24B);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private float keeperShownX() {
        // 结果展示阶段门将会扑向他实际选择的一侧（重新随机展示）
        return 180 + (float) Math.sin(now() / 90.0) * 60;
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
