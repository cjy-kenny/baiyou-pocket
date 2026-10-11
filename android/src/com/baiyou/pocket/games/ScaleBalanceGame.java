package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g022 天平砝码：从砝码中挑出让总重恰好等于目标的组合，点"称重"确认。
 *  正好平衡 +150 进入下一题；不平扣命；3 命尽结算。 */
public class ScaleBalanceGame extends CanvasGame {
    private final Random rnd = new Random();
    private final int[] weights = new int[6];
    private final boolean[] picked = new boolean[6];
    private int target, lives = 3, shownScore, solved;
    private boolean settling;

    public ScaleBalanceGame() {
        super(360, 560);
        genPuzzle();
    }

    private void genPuzzle() {
        for (int i = 0; i < 6; i++) weights[i] = 1 + rnd.nextInt(30);
        for (boolean b : picked) b = false;
        // 目标 = 随机子集和（保证有解）
        target = 0;
        for (int i = 0; i < 6; i++)
            if (rnd.nextInt(3) > 0) {
                target += weights[i];
                picked[i] = true; // 预置一个参考解的选择（玩家重置）
            }
        if (target == 0) target = weights[0];
        java.util.Arrays.fill(picked, false);
    }

    private void startGame() {
        lives = 3;
        shownScore = 0;
        solved = 0;
        settling = false;
        over = false;
        genPuzzle();
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private int sum() {
        int s = 0;
        for (int i = 0; i < 6; i++) if (picked[i]) s += weights[i];
        return s;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        for (int i = 0; i < 6; i++) {
            float wx = 34 + (i % 3) * 100, wy = 250 + (i / 3) * 90;
            if (hit(x, y, wx, wy, 88, 72)) {
                picked[i] = !picked[i];
                beep(0);
                dirty();
                return;
            }
        }
        if (hit(x, y, 70, 440, 220, 60)) {
            weigh();
        }
    }

    private void weigh() {
        int s = sum();
        if (s == target) {
            solved++;
            shownScore += 150;
            host.setScore(shownScore);
            beep(3);
            popup("平衡！ +150", lw / 2f, 180, 0xFF7CFFB2, 20);
            burst(lw / 2f, 210, 0xFF7CFFB2, 14, 6f);
            postDelayedSafe(() -> {
                if (over || settling) return;
                genPuzzle();
                dirty();
            }, 800);
        } else {
            lives--;
            beep(2);
            shake(200);
            popup(s > target ? "超重了！" : "不够重！", lw / 2f, 180, 0xFFE8574F, 17);
            if (lives <= 0) {
                settling = true;
                over = true;
                stopLoop();
                postDelayedSafe(() -> host.gameOver(shownScore), 850);
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF201E18);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "已平衡 " + solved + " 次", 40, 30, 13, 0xFFECECF1, true);
        // 天平
        float beamY = 150;
        rr(c, 40, beamY, lw - 40, beamY + 10, 4, 0xFF6B5E42);
        rr(c, lw / 2f - 8, beamY + 10, lw / 2f + 8, beamY + 60, 4, 0xFF6B5E42);
        rr(c, lw / 2f - 60, beamY + 58, lw / 2f + 60, beamY + 70, 6, 0xFF6B5E42);
        label(c, "目标 " + target + " g", lw / 2f, beamY - 26, 20, 0xFFFFC24B, true);
        int tilt = Integer.compare(sum(), target);
        label(c, tilt == 0 ? "⚖ 平衡" : (tilt > 0 ? "左倾 ↓" : "右倾 ↓"),
                lw / 2f, beamY + 92, 16, tilt == 0 ? 0xFF7CFFB2 : 0xFFE8848C, true);
        label(c, "当前 " + sum() + " g", lw / 2f, beamY + 118, 14, 0xFFB9C4E0, false);
        // 砝码
        for (int i = 0; i < 6; i++) {
            float wx = 34 + (i % 3) * 100, wy = 250 + (i / 3) * 90;
            rr(c, wx, wy, wx + 88, wy + 72, 10, picked[i] ? 0xFFFFC24B : 0xFF3A362C);
            label(c, weights[i] + "g", wx + 44, wy + 36, 22, picked[i] ? 0xFF1B1C22 : 0xFFECECF1, true);
        }
        // 称重按钮
        rr(c, 70, 440, 290, 500, 14, 0xFF7B7FF5);
        label(c, "称 重", lw / 2f, 470, 22, 0xFFFFFFFF, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
