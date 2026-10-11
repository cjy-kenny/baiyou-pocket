package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g045 瞬时记忆：一串数字一闪而过，从四个候选串里挑出刚才那串；
 *  位数一轮加一、闪现越来越短，错一次结束。 */
public class NumberFlashGame extends CanvasGame {
    private static final int TOTAL_ROUNDS = 8;

    private final Random rnd = new Random();
    private String shown, answer;
    private final List<String> opts = new ArrayList<>();
    private int ansIdx, round;
    private boolean showing, settling;
    private long showUntil;
    private int shownScore;

    public NumberFlashGame() {
        super(360, 560);
        nextRound();
    }

    private void nextRound() {
        round++;
        int digits = 2 + round; // 3..10 位
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) sb.append(rnd.nextInt(10));
        answer = sb.toString();
        shown = answer;
        opts.clear();
        opts.add(answer);
        int guard = 0;
        while (opts.size() < 4 && guard++ < 60) {
            StringBuilder alt = new StringBuilder(answer);
            int changes = 1 + rnd.nextInt(2);
            for (int k = 0; k < changes; k++) {
                int p = rnd.nextInt(alt.length());
                alt.setCharAt(p, (char) ('0' + rnd.nextInt(10)));
            }
            if (!opts.contains(alt.toString())) opts.add(alt.toString());
        }
        while (opts.size() < 4) opts.add("999" + opts.size());
        java.util.Collections.shuffle(opts, rnd);
        ansIdx = opts.indexOf(answer);
        showing = true;
        showUntil = now() + Math.max(420, 1150 - round * 90);
    }

    private void startGame() {
        round = 0;
        shownScore = 0;
        settling = false;
        over = false;
        nextRound();
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || showing || action != 0) return;
        for (int i = 0; i < 4; i++) {
            float oy = 260 + i * 68;
            if (hit(x, y, 30, oy, 300, 56)) {
                if (i == ansIdx) {
                    shownScore += 100 + round * 20;
                    host.setScore(shownScore);
                    beep(1);
                    burst(180, oy + 28, 0xFF7CFFB2, 8, 4f);
                    if (round >= TOTAL_ROUNDS) {
                        over = true;
                        stopLoop();
                        popup("满分记忆！", lw / 2f, lh / 2f, 0xFFFFC24B, 20);
                        dirty();
                        postDelayedSafe(() -> host.gameOver(shownScore), 850);
                        return;
                    }
                    settling = true;
                    postDelayedSafe(() -> {
                        settling = false;
                        if (over) return;
                        nextRound();
                        dirty();
                    }, 450);
                } else {
                    over = true;
                    stopLoop();
                    beep(2);
                    shake(220);
                    popup("第 " + round + " 轮看岔了", lw / 2f, lh / 2f, 0xFFE8574F, 17);
                    int finalScore = (round - 1) * 100;
                    host.setScore(finalScore);
                    dirty();
                    postDelayedSafe(() -> host.gameOver(finalScore), 850);
                }
                return;
            }
        }
    }

    @Override
    protected void onTick() {
        if (showing && now() >= showUntil) showing = false;
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF14201C);
        label(c, "第 " + Math.min(round, TOTAL_ROUNDS) + "/" + TOTAL_ROUNDS + " 轮",
                lw / 2f, 60, 14, 0xFF9ACDAA, false);
        if (showing) {
            label(c, "记住这串数字", lw / 2f, 150, 14, 0xFF8B8E9C, false);
            label(c, shown, lw / 2f, 200, 34, 0xFFECECF1, true);
        } else {
            label(c, "刚才那串是？", lw / 2f, 190, 16, 0xFFECECF1, true);
            for (int i = 0; i < 4; i++) {
                float oy = 260 + i * 68;
                rr(c, 30, oy, 330, oy + 56, 10, 0xFF24352E);
                label(c, opts.get(i), 180, oy + 28, 22, 0xFFECECF1, true);
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
