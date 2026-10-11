package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g099 部首组字：两个部件拼出哪个字？（原创组合题） */
public class RadicalQuizGame extends QuizGame {
    private static final String[][] COMBOS = {
            {"木", "目", "相"}, {"日", "月", "明"}, {"人", "言", "信"},
            {"小", "大", "尖"}, {"山", "石", "岩"}, {"田", "力", "男"},
            {"女", "子", "好"}, {"不", "正", "歪"}, {"鱼", "羊", "鲜"},
            {"门", "口", "问"}, {"口", "鸟", "鸣"}, {"土", "也", "地"},
            {"白", "水", "泉"}, {"火", "因", "烟"}, {"米", "女", "数"}};
    private static final String[] FILLERS = {"林", "森", "晴", "忙", "看", "想", "跑", "吃"};

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();

    public RadicalQuizGame() {
        super();
    }

    @Override
    protected void next() {
        if (used.size() >= COMBOS.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(COMBOS.length);
        } while (used.contains(i));
        used.add(i);
        String correct = COMBOS[i][2];
        List<String> pool = new ArrayList<>();
        for (String[] c : COMBOS)
            if (!c[2].equals(correct)) pool.add(c[2]);
        for (String f : FILLERS) if (!pool.contains(f)) pool.add(f);
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ(COMBOS[i][0] + " + " + COMBOS[i][1] + " = ？", opts.toArray(new String[0]), ans);
    }
}
