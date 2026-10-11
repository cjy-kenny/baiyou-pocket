package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g016 心算冲刺：限时连续心算，答对连击升档（加→减→乘→混合），
 *  分数随档位翻涨，答错退一档并扣命。 */
public class MentalRushGame extends QuizGame {
    private final Random rnd = new Random();
    private int tier;

    public MentalRushGame() {
        super();
    }

    @Override
    protected int baseScore() {
        return 10 + tier * 4;
    }

    @Override
    protected void next() {
        int a, b, ans;
        String expr;
        int op = Math.min(tier, 3) == 3 ? rnd.nextInt(4) : Math.min(tier, 3);
        switch (op) {
            case 0:
                a = 10 + rnd.nextInt(40 + tier * 10);
                b = 10 + rnd.nextInt(40 + tier * 10);
                ans = a + b;
                expr = a + " + " + b + " = ?";
                break;
            case 1:
                a = 30 + rnd.nextInt(60);
                b = 5 + rnd.nextInt(40);
                if (b > a) {
                    int t = a;
                    a = b;
                    b = t;
                }
                ans = a - b;
                expr = a + " - " + b + " = ?";
                break;
            case 2:
                a = 3 + rnd.nextInt(9 + tier);
                b = 3 + rnd.nextInt(9 + tier);
                ans = a * b;
                expr = a + " × " + b + " = ?";
                break;
            default:
                a = 6 + rnd.nextInt(30);
                b = 2 + rnd.nextInt(9);
                ans = a * b - rnd.nextInt(5);
                expr = a + " × " + b + " - ? = " + (a * b - ans);
                break;
        }
        List<String> opts = new ArrayList<>();
        String correct = String.valueOf(ans);
        opts.add(correct);
        int guard = 0;
        while (opts.size() < 4 && guard++ < 40) {
            int delta = rnd.nextInt(9) - 4;
            if (delta == 0) delta = 2;
            String s = String.valueOf(ans + delta);
            if (!opts.contains(s) && ans + delta >= 0) opts.add(s);
        }
        while (opts.size() < 4) opts.add(String.valueOf(ans + opts.size() * 11 + 3));
        java.util.Collections.shuffle(opts, rnd);
        setQ(expr, opts.toArray(new String[0]), opts.indexOf(correct));
    }

    @Override
    protected void onAnswered(boolean correct, int gained) {
        if (correct) {
            tier = Math.min(4, tier + 1);
            if (tier > 0) popup("升档 ×" + (tier + 1), 180, 210, 0xFFFFC24B, 13);
        } else {
            tier = Math.max(0, tier - 1);
        }
    }
}
