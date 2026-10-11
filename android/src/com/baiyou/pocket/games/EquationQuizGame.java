package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g015 算式速配：算式缺一个数，从四个候选里挑出使等式成立者。
 *  连对加分，答错扣命；难度随连对上升。 */
public class EquationQuizGame extends QuizGame {
    private final Random rnd = new Random();
    private int level;

    public EquationQuizGame() {
        super();
    }

    @Override
    protected int baseScore() {
        return 10 + level;
    }

    @Override
    protected void next() {
        level = Math.min(6, streakLevel());
        boolean hard = level >= 3;
        int max = hard ? 20 + level * 5 : 10;
        int a = 2 + rnd.nextInt(max);
        int b = 1 + rnd.nextInt(max);
        int op = hard ? rnd.nextInt(3) : rnd.nextInt(2); // 0+ 1- 2×
        int ans;
        String expr;
        switch (op) {
            case 0:
                ans = a + b;
                expr = a + " + ? = " + ans;
                break;
            case 1:
                if (b > a) {
                    int t = a;
                    a = b;
                    b = t;
                }
                ans = a - b;
                expr = a + " - ? = " + ans;
                break;
            default:
                b = 2 + rnd.nextInt(Math.min(9, max / 2));
                ans = a * b;
                expr = a + " × ? = " + ans;
                break;
        }
        // 干扰项
        List<String> opts = new ArrayList<>();
        String correct = String.valueOf(ans);
        opts.add(correct);
        int guard = 0;
        while (opts.size() < 4 && guard++ < 40) {
            int delta = (rnd.nextInt(7) - 3);
            if (delta == 0) delta = 1;
            int v = ans + delta;
            if (v < 0 || v == ans) continue;
            String s = String.valueOf(v);
            if (!opts.contains(s)) opts.add(s);
        }
        while (opts.size() < 4) opts.add(String.valueOf(ans + opts.size() * 7 + 1));
        java.util.Collections.shuffle(opts, rnd);
        setQ(expr, opts.toArray(new String[0]), opts.indexOf(correct));
    }

    /** QuizGame 的 streak 是 private，这里用已连对次数的近似（由回调维护）。 */
    private int streakLevel;

    private int streakLevel() {
        return streakLevel;
    }

    @Override
    protected void onAnswered(boolean correct, int gained) {
        if (correct) streakLevel++;
        else streakLevel = 0;
    }
}
