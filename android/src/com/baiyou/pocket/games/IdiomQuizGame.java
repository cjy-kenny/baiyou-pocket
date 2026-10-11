package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g091 成语填空：补全成语里缺的字（成语为公有领域文化内容）。 */
public class IdiomQuizGame extends QuizGame {
    private static final String[][] IDIOMS = {
            {"一箭双_", "雕"}, {"画蛇添_", "足"}, {"亡羊补_", "牢"},
            {"守株待_", "兔"}, {"掩耳盗_", "铃"}, {"井底之_", "蛙"},
            {"对牛弹_", "琴"}, {"杯弓蛇_", "影"}, {"狐假虎_", "威"},
            {"刻舟求_", "剑"}, {"南辕北_", "辙"}, {"东施效_", "颦"},
            {"塞翁失_", "马"}, {"叶公好_", "龙"}, {"滥竽充_", "数"},
            {"拔苗助_", "长"}, {"惊弓之_", "鸟"}, {"指鹿为_", "马"},
            {"卧薪尝_", "胆"}, {"破釜沉_", "舟"}, {"完璧归_", "赵"},
            {"负荆请_", "罪"}, {"纸上谈_", "兵"}, {"三顾茅_", "庐"},
            {"望梅止_", "渴"}, {"闻鸡起_", "舞"}, {"悬梁刺_", "股"},
            {"胸有成_", "竹"}, {"雪中送_", "炭"}, {"锦上添_", "花"},
    };

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();

    public IdiomQuizGame() {
        super();
    }

    @Override
    protected void next() {
        if (used.size() >= IDIOMS.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(IDIOMS.length);
        } while (used.contains(i));
        used.add(i);
        String word = IDIOMS[i][0];
        String correct = IDIOMS[i][1];
        // 干扰字：从其它成语答案里取
        List<String> pool = new ArrayList<>();
        for (String[] idiom : IDIOMS)
            if (!idiom[1].equals(correct)) pool.add(idiom[1]);
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ("补全成语：" + word, opts.toArray(new String[0]), ans);
    }
}
