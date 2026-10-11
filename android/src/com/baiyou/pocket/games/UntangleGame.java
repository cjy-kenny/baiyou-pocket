package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g066 抽绳解谜：左右两列钉子间拉着乱绳，点两根绳交换右端，
 *  把交叉数清零即过关（90 秒多关）。 */
public class UntangleGame extends CanvasGame {
    private static final int ROPES = 6;
    private static final float LX = 60, RX = 300, TOP = 140, GAP = 62;

    private final Random rnd = new Random();
    private final int[] right = new int[ROPES];   // 左端 i → 右端 right[i]
    private int level, shownScore, swaps;
    private long roundEnd;
    private int selRope = -1;
    private boolean settling;

    public UntangleGame() {
        super(360, 560);
        genLevel();
    }

    private void genLevel() {
        for (int i = 0; i < ROPES; i++) right[i] = i;
        // 打乱（保证有交叉）
        do {
            for (int i = ROPES - 1; i > 0; i--) {
                int j = rnd.nextInt(i + 1);
                int t = right[i];
                right[i] = right[j];
                right[j] = t;
            }
        } while (crossings() == 0);
        swaps = 0;
    }

    private int crossings() {
        int n = 0;
        for (int i = 0; i < ROPES; i++)
            for (int j = i + 1; j < ROPES; j++) {
                // 顺序对 (i<j) 交叉当 right[i] > right[j]
                if (right[i] > right[j]) n++;
            }
        return n;
    }

    private void startRound() {
        level = 1;
        shownScore = 0;
        roundEnd = now() + 90000;
        genLevel();
        selRope = -1;
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(100);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(100);
    }

    private float leftY(int i) {
        return TOP + i * GAP;
    }

    private float rightY(int i) {
        return TOP + i * GAP;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 点左端选中，点左端另一根交换右端
        int hit2 = -1;
        for (int i = 0; i < ROPES; i++)
            if (hit(x, y, LX - 26, leftY(i) - 20, 52, 40)) hit2 = i;
        if (hit2 < 0) return;
        if (selRope < 0) {
            selRope = hit2;
            beep(0);
        } else if (selRope == hit2) {
            selRope = -1;
            beep(0);
        } else {
            int t = right[selRope];
            right[selRope] = right[hit2];
            right[hit2] = t;
            swaps++;
            selRope = -1;
            beep(1);
            int cr = crossings();
            if (cr == 0) {
                shownScore += 200 + swaps * 10;
                host.setScore(shownScore);
                level++;
                popup("解开！ +" + (200 + swaps * 10), lw / 2f, 100, 0xFF7CFFB2, 18);
                burst(RX, TOP + GAP * 2.5f, 0xFF7CFFB2, 18, 7f);
                beep(3);
                genLevel();
            } else {
                popup("还剩 " + cr + " 处交叉", lw / 2f, 100, 0xFFECECF1, 14);
            }
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF121A22);
        float remain = Math.max(0f, (roundEnd - now()) / 90000f);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "交叉 " + crossings() + " 处 · 换绳 " + swaps + " 次",
                lw - 30, 30, 12, 0xFF9AA0AC, true);
        for (int i = 0; i < ROPES; i++) {
            stroke.setColor(i == selRope ? 0xFFFFC24B : 0xFF5B8DEF);
            stroke.setStrokeWidth(4);
            c.drawLine(LX, leftY(i), RX, rightY(right[i]), stroke);
        }
        for (int i = 0; i < ROPES; i++) {
            fill.setColor(0xFF9AA0AC);
            c.drawCircle(LX, leftY(i), 10, fill);
            c.drawCircle(RX, rightY(right[i]), 10, fill);
            label(c, String.valueOf(right[i] + 1), RX, rightY(right[i]), 12, 0xFF1B1C22, true);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
