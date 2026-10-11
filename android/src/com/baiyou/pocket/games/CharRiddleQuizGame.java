package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g092 字谜猜猜：根据原创谜面猜单个汉字。 */
public class CharRiddleQuizGame extends QuizGame {
    private static final String[][] RIDDLES = {
            {"一口咬掉牛尾巴", "告"},
            {"十张口，一颗心", "思"},
            {"太阳西边下，月儿东边挂", "明"},
            {"王姑娘，白姑娘，坐在石头上", "碧"},
            {"一人一张口，口下长只手", "拿"},
            {"半青半紫", "素"},
            {"半个月亮", "胖"},
            {"三人同日去看花", "春"},
            {"综合门市", "闹"},
            {"需要一半，留下一半", "雷"},
            {"上不在上，下不在下", "一"},
            {"千里相逢", "重"},
            {"客满", "促"},
            {"重逢", "观"},
            {"两人土上坐", "坐"},
            {"一半是水，一半是田", "油"},
            {"加倍才有劲", "力"},
            {"多一半", "夕"},
    };

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();

    public CharRiddleQuizGame() {
        super();
    }

    @Override
    protected void next() {
        if (used.size() >= RIDDLES.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(RIDDLES.length);
        } while (used.contains(i));
        used.add(i);
        String correct = RIDDLES[i][1];
        List<String> pool = new ArrayList<>();
        for (String[] r : RIDDLES)
            if (!r[1].equals(correct)) pool.add(r[1]);
        // 补足常见汉字干扰
        String[] extra = {"大", "小", "口", "木", "水", "火", "土", "月"};
        for (String e : extra) if (pool.size() < 10 && !pool.contains(e)) pool.add(e);
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ(RIDDLES[i][0] + "（打一字）", opts.toArray(new String[0]), ans);
    }
}
