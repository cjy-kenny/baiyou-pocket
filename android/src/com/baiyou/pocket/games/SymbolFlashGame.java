package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g044 图案速记：一个符号一闪而过，从四个候选里挑出它；
 *  闪现越来越短，全程十轮，错一次即结束。 */
public class SymbolFlashGame extends CanvasGame {
    private static final String GLYPHS = "●■▲◆★✚◐❖";
    private static final int TOTAL_ROUNDS = 10;

    private final Random rnd = new Random();
    private String shown, answer;
    private final List<String> opts = new ArrayList<>();
    private int ansIdx, round;
    private boolean showing, settling;
    private long showUntil;
    private int shownScore;

    public SymbolFlashGame() {
        super(360, 560);
        nextRound();
    }

    private void nextRound() {
        round++;
        int a = rnd.nextInt(GLYPHS.length());
        shown = String.valueOf(GLYPHS.charAt(a));
        answer = shown;
        // 选项：正确 + 3 个不同符号
        List<Integer> idxs = new ArrayList<>();
        for (int i = 0; i < GLYPHS.length(); i++) if (i != a) idxs.add(i);
        java.util.Collections.shuffle(idxs, rnd);
        List<String> all = new ArrayList<>();
        all.add(answer);
        for (int i = 0; i < 3; i++) all.add(String.valueOf(GLYPHS.charAt(idxs.get(i))));
        java.util.Collections.shuffle(all, rnd);
        opts.clear();
        opts.addAll(all);
        ansIdx = opts.indexOf(answer);
        showing = true;
        // 闪现时长随轮数缩短：900ms → 380ms
        showUntil = now() + Math.max(380, 950 - round * 60);
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
            float ox = 34 + (i % 2) * 156, oy = 280 + (i / 2) * 130;
            if (hit(x, y, ox, oy, 136, 112)) {
                if (i == ansIdx) {
                    shownScore += 100;
                    host.setScore(shownScore);
                    beep(1);
                    burst(ox + 68, oy + 56, 0xFF7CFFB2, 10, 5f);
                    if (round >= TOTAL_ROUNDS) {
                        over = true;
                        stopLoop();
                        popup("十轮全对！", lw / 2f, lh / 2f, 0xFFFFC24B, 20);
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
                    }, 500);
                } else {
                    over = true;
                    stopLoop();
                    beep(2);
                    shake(220);
                    popup("第 " + round + " 轮记岔了", lw / 2f, lh / 2f, 0xFFE8574F, 17);
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
        rr(c, 0, 0, lw, lh, 0, 0xFF1E1B26);
        label(c, "第 " + Math.min(round, TOTAL_ROUNDS) + "/" + TOTAL_ROUNDS + " 轮",
                lw / 2f, 60, 14, 0xFFB9AED0, false);
        if (showing) {
            label(c, "看好了", lw / 2f, 160, 15, 0xFFFFC24B, true);
            label(c, shown, lw / 2f, 230, 80, 0xFFECECF1, true);
        } else {
            label(c, "刚才闪过的是？", lw / 2f, 180, 16, 0xFFECECF1, true);
            for (int i = 0; i < 4; i++) {
                float ox = 34 + (i % 2) * 156, oy = 280 + (i / 2) * 130;
                rr(c, ox, oy, ox + 136, oy + 112, 12, 0xFF2C2836);
                label(c, opts.get(i), ox + 68, oy + 56, 44, 0xFFECECF1, true);
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
