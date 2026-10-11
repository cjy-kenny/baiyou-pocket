package com.baiyou.pocket.game;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

/** 题库/问答类家族基类：题面 + 2~4 个选项 + 3 条命 + 60 秒回合制。
 *  子类实现 next() 出题（文字题库、口算生成等），答对连击加分，答错扣命，
 *  命尽或超时结算。 */
public abstract class QuizGame extends CanvasGame {
    public static final long ROUND_MS = 60000;

    private static final int OPT_H = 62, OPT_GAP = 12;

    private String question = "";
    private final String[] options = new String[4];
    private int optCount, answer;
    private int score, lives = 3, streak, bestStreak;
    private long roundEnd;
    private long flashUntil;
    private int flashOpt = -1;
    private boolean flashOk;

    protected QuizGame() {
        super(360, 560);
    }

    /** 子类出一题：调 setQ()。 */
    protected abstract void next();

    protected final void setQ(String q, String[] opts, int ans) {
        question = q;
        optCount = Math.min(4, opts.length);
        for (int i = 0; i < optCount; i++) options[i] = opts[i];
        answer = ans;
    }

    /** 答题后的回调（子类可扩展音效/文案）。 */
    protected void onAnswered(boolean correct, int gained) {
    }

    /** 每题基础分（默认 10）。 */
    protected int baseScore() {
        return 10;
    }

    @Override
    public void restartGame() {
        score = 0;
        lives = 3;
        streak = 0;
        bestStreak = 0;
        roundEnd = now() + ROUND_MS;
        over = false;
        flashUntil = 0;
        host.setScore(0);
        next();
        startLoop(100);
        dirty();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(100);
    }

    private float optH() {
        return optCount <= 2 ? 72 : OPT_H;
    }

    private float optX(int i) {
        return optCount <= 2 ? 40 : (i % 2 == 0 ? 22 : 188);
    }

    private float optY(int i) {
        float top = 250;
        if (optCount <= 2) return top + i * (optH() + OPT_GAP);
        return top + (i / 2) * (optH() + OPT_GAP);
    }

    private float optW() {
        return optCount <= 2 ? 280 : 150;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || action != 0 || now() < flashUntil) return;
        for (int i = 0; i < optCount; i++) {
            if (hit(x, y, optX(i), optY(i), optW(), optH())) {
                answer(i);
                return;
            }
        }
    }

    private void answer(int i) {
        boolean correct = i == answer;
        if (correct) {
            streak++;
            bestStreak = Math.max(bestStreak, streak);
            int gained = baseScore() * (1 + Math.min(4, streak - 1));
            score += gained;
            host.setScore(score);
            flashOpt = i;
            flashOk = true;
            flashUntil = now() + 320;
            popup("+" + gained, 180, 300, 0xFF7CFFB2, 22);
            if (streak >= 3) popup("连对 ×" + streak, 180, 268, 0xFFFFC24B, 14);
            beep(1);
            burst(optX(i) + optW() / 2f, optY(i) + optH() / 2f, 0xFF7CFFB2, 8, 4f);
            onAnswered(true, gained);
        } else {
            streak = 0;
            lives--;
            flashOpt = i;
            flashOk = false;
            flashUntil = now() + 420;
            beep(2);
            shake(180);
            onAnswered(false, 0);
        }
        if (lives <= 0) {
            finish("生命用完了");
            return;
        }
        dirty();
    }

    private void finish(String why) {
        over = true;
        stopLoop();
        popup(why + " 得分 " + score, 180, 200, 0xFFECECF1, 18);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 800);
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            finish("时间到");
            return;
        }
        if (flashUntil > 0 && now() >= flashUntil) {
            flashUntil = 0;
            flashOpt = -1;
            if (flashOk) next(); // 答对闪过后出下一题；答错停留原题
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF232733);

        // HUD：得分 / 生命 / 倒计时条
        label(c, "得分 " + score, 44, 26, 15, 0xFFECECF1, true);
        hearts(c, lives, 3, lw - 60, 26, 7);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 46, lw - 16, 54, 4, 0xFF2A2D37);
        rr(c, 16, 46, 16 + (lw - 32) * remain, 54, 4, remain < 0.25f ? 0xFFE8574F : 0xFF7B7FF5);

        // 题面（最多 4 行）
        String[] lines = wrapCn(question, 13);
        float qy = 100;
        for (int i = 0; i < Math.min(4, lines.length); i++) {
            label(c, lines[i], lw / 2f, qy, 24, 0xFFECECF1, true);
            qy += 40;
        }

        // 选项
        for (int i = 0; i < optCount; i++) {
            int bg = 0xFF323748;
            if (i == flashOpt) bg = flashOk ? 0xFF2FA36B : 0xFFE8574F;
            rr(c, optX(i), optY(i), optX(i) + optW(), optY(i) + optH(), 10, bg);
            label(c, options[i], optX(i) + optW() / 2f, optY(i) + optH() / 2f, 19, 0xFFF4F5F9, true);
        }

        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    /** 选项乱序工具：把候选打乱并返回正确项的新下标。 */
    protected static int shuffleOpts(List<String> opts, String correct, List<String> pool,
                                     int n, java.util.Random rnd) {
        List<String> chosen = new ArrayList<>();
        chosen.add(correct);
        List<String> rest = new ArrayList<>(pool);
        java.util.Collections.shuffle(rest, rnd);
        for (String s : rest) {
            if (chosen.size() >= n) break;
            if (!chosen.contains(s)) chosen.add(s);
        }
        java.util.Collections.shuffle(chosen, rnd);
        opts.clear();
        opts.addAll(chosen);
        return opts.indexOf(correct);
    }
}
