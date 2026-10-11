package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g093 诗词接句：接出名句的下一句（均为公有领域古诗词）。 */
public class PoemQuizGame extends QuizGame {
    private static final String[][] POEMS = {
            {"床前明月光", "疑是地上霜"},
            {"春眠不觉晓", "处处闻啼鸟"},
            {"锄禾日当午", "汗滴禾下土"},
            {"白日依山尽", "黄河入海流"},
            {"举头望明月", "低头思故乡"},
            {"欲穷千里目", "更上一层楼"},
            {"空山不见人", "但闻人语响"},
            {"好雨知时节", "当春乃发生"},
            {"两个黄鹂鸣翠柳", "一行白鹭上青天"},
            {"接天莲叶无穷碧", "映日荷花别样红"},
            {"不识庐山真面目", "只缘身在此山中"},
            {"落霞与孤鹜齐飞", "秋水共长天一色"},
            {"海内存知己", "天涯若比邻"},
            {"谁知盘中餐", "粒粒皆辛苦"},
            {"野火烧不尽", "春风吹又生"},
            {"独在异乡为异客", "每逢佳节倍思亲"},
    };

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();

    public PoemQuizGame() {
        super();
    }

    @Override
    protected void next() {
        if (used.size() >= POEMS.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(POEMS.length);
        } while (used.contains(i));
        used.add(i);
        String correct = POEMS[i][1];
        List<String> pool = new ArrayList<>();
        for (String[] p : POEMS)
            if (!p[1].equals(correct)) pool.add(p[1]);
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ("「" + POEMS[i][0] + "」的下一句是？", opts.toArray(new String[0]), ans);
    }
}
