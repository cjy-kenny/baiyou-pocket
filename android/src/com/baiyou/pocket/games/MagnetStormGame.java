package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g090 磁暴连射：点任意敌点释放磁暴，连锁清除附近敌点；
 *  一次清得越多奖励越高，45 秒内清场越多越好。 */
public class MagnetStormGame extends CanvasGame {
    private static final long ROUND_MS = 45000;
    private static final float CHAIN_R = 92;

    private final List<float[]> dots = new ArrayList<>(); // {x,y}
    private final Random rnd = new Random();
    private int shownScore, cleared;
    private long roundEnd;
    private boolean settling;

    public MagnetStormGame() {
        super(360, 560);
        genWave();
    }

    private void genWave() {
        dots.clear();
        int n = 10 + cleared / 6;
        while (dots.size() < n) {
            float x = rand(36, lw - 36), y = rand(120, lh - 100);
            boolean near = false;
            for (float[] d : dots) {
                float dx = d[0] - x, dy = d[1] - y;
                if (dx * dx + dy * dy < 55 * 55) near = true;
            }
            if (!near) dots.add(new float[]{x, y});
        }
    }

    private void startRound() {
        shownScore = 0;
        cleared = 0;
        roundEnd = now() + ROUND_MS;
        genWave();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int hit2 = -1;
        for (int i = 0; i < dots.size(); i++) {
            float dx = x - dots.get(i)[0], dy = y - dots.get(i)[1];
            if (dx * dx + dy * dy < 26 * 26) {
                hit2 = i;
                break;
            }
        }
        if (hit2 < 0) return;
        // 连锁清除
        List<Integer> chain = new ArrayList<>();
        chain.add(hit2);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int i = 0; i < dots.size(); i++) {
                if (chain.contains(i)) continue;
                for (int j : chain) {
                    float dx = dots.get(i)[0] - dots.get(j)[0];
                    float dy = dots.get(i)[1] - dots.get(j)[1];
                    if (dx * dx + dy * dy < CHAIN_R * CHAIN_R) {
                        chain.add(i);
                        grew = true;
                        break;
                    }
                }
            }
        }
        // 移除（从大到小）
        chain.sort((a, b) -> b - a);
        for (int i : chain) {
            burst(dots.get(i)[0], dots.get(i)[1], 0xFF7CFFB2, 8, 5f);
            dots.remove(i);
        }
        cleared += chain.size();
        int gain = chain.size() * 15 + (chain.size() >= 4 ? (chain.size() - 3) * 25 : 0);
        shownScore += gain;
        host.setScore(shownScore);
        beep(chain.size() >= 3 ? 3 : 1);
        popup("连锁 ×" + chain.size() + "  +" + gain, lw / 2f, 100, 0xFF7CFFB2, 17);
        if (dots.isEmpty()) {
            popup("清场！ +200", lw / 2f, 140, 0xFFFFC24B, 18);
            shownScore += 200;
            host.setScore(shownScore);
            genWave();
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("清除 " + cleared + " 点", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF141026);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF9B59F6);
        label(c, "清除 " + cleared + " 点", 40, 30, 14, 0xFFB9AED0, true);
        for (float[] d : dots) {
            fill.setColor(0xFF9B59F6);
            c.drawCircle(d[0], d[1], 15, fill);
            fill.setColor(0xFFE8848C);
            c.drawCircle(d[0], d[1], 7, fill);
            stroke.setColor(0x337CFFB2);
            stroke.setStrokeWidth(1);
            rf.set(d[0] - CHAIN_R, d[1] - CHAIN_R, d[0] + CHAIN_R, d[1] + CHAIN_R);
            c.drawCircle(d[0], d[1], CHAIN_R, stroke);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
