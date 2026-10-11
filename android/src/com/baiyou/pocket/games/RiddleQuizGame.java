package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g098 谜语小铺：原创谜面猜日常事物。 */
public class RiddleQuizGame extends QuizGame {
    private static final String[][] RIDDLES = {
            {"越洗越脏，不洗不能尝", "水"},
            {"有面没有口，有脚没有手，教你写字算数它都有", "书"},
            {"小小房子路边站，行人下雨里面钻", "亭"},
            {"一匹马儿两人骑，高高兴兴上学去", "车"},
            {"五个兄弟住一起，名字不同高不齐", "手指"},
            {"不怕冷也不怕热，整天挂在墙上滴答说", "钟"},
            {"身穿花衣爱打扮，一对翅膀亮闪闪（打一昆虫）", "蝴蝶"},
            {"圆形天空挂，夜里洒月光", "月亮"},
            {"千根线来万根线，落到水里就不见", "雨"},
            {"一座桥，地上架，走桥的人不用脚", "斑马线"},
            {"小小两次方盒子，看看它能知天下", "手机"},
            {"四四方方一间房，烈火屋里把歌放", "音箱"},
            {"风来它就转，风停它就站", "风车"},
            {"白天睡觉，晚上上班，能把黑夜变白天", "灯"},
    };

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();

    public RiddleQuizGame() {
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
        String[] extra = {"雨伞", "帽子", "桌子", "苹果", "小猫", "足球"};
        for (String e : extra) if (!pool.contains(e)) pool.add(e);
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ(RIDDLES[i][0] + "（打一物）", opts.toArray(new String[0]), ans);
    }
}
