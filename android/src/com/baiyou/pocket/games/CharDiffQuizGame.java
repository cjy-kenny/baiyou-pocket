package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g095 汉字找茬：给定一个汉字，从候选里挑出「长得最像但不是它」的字。 */
public class CharDiffQuizGame extends QuizGame {
    private static final String[][] PAIRS = {
            {"己", "已"}, {"未", "末"}, {"土", "士"}, {"千", "干"},
            {"天", "夫"}, {"木", "本"}, {"大", "犬"}, {"日", "目"},
            {"田", "由"}, {"刀", "刃"}, {"乌", "鸟"}, {"免", "兔"},
            {"拨", "拔"}, {"折", "析"}, {"候", "侯"}, {"眯", "咪"},
    };

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();

    public CharDiffQuizGame() {
        super();
    }

    @Override
    protected void next() {
        if (used.size() >= PAIRS.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(PAIRS.length);
        } while (used.contains(i));
        used.add(i);
        String base = PAIRS[i][0];
        String correct = PAIRS[i][1];
        // 干扰项：其它不相似的常见字
        List<String> pool = new ArrayList<>();
        String[] extra = {"口", "山", "水", "火", "云", "风", "石", "田", "王", "车"};
        for (String[] p : PAIRS) {
            if (!p[0].equals(base) && !p[1].equals(correct)) pool.add(p[0]);
        }
        for (String e : extra) if (!pool.contains(e)) pool.add(e);
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ("哪个不是「" + base + "」，只是长得像？", opts.toArray(new String[0]), ans);
    }
}
