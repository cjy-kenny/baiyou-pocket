package com.baiyou.pocket.games;

import com.baiyou.pocket.game.QuizGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g094 字母词链：给出单词，选出以它尾字母开头的下一个单词。
 *  出题保证该尾字母下「恰好只有一个」合法接词，避免歧义。 */
public class WordChainQuizGame extends QuizGame {
    private static final String[] WORDS = {
            "APPLE", "ELEPHANT", "TIGER", "RABBIT", "TOMATO", "ORANGE", "EGG", "GIRAFFE",
            "EAGLE", "LEMON", "NOSE", "EARTH", "HEART", "TRAIN", "NOODLE", "EAR",
            "RAIN", "NORTH", "HOUSE", "ECHO", "OCEAN", "NEST", "ROSE", "SUN",
    };

    private final Random rnd = new Random();

    public WordChainQuizGame() {
        super();
    }

    @Override
    protected void next() {
        // 找「唯一可接」的词对：question 尾字母只匹配一个候选
        List<int[]> pairs = new ArrayList<>(); // {questionIdx, answerIdx}
        for (int i = 0; i < WORDS.length; i++) {
            char last = WORDS[i].charAt(WORDS[i].length() - 1);
            int match = -1, count = 0;
            for (int j = 0; j < WORDS.length; j++) {
                if (j == i) continue;
                if (WORDS[j].charAt(0) == last) {
                    count++;
                    match = j;
                }
            }
            if (count == 1) pairs.add(new int[]{i, match});
        }
        if (pairs.isEmpty()) {
            // 数据兜底（不会发生）：固定题目
            setQ("APPLE  →  ?（接尾字母）",
                    new String[]{"ELEPHANT", "TIGER", "SUN", "MILK"}, 0);
            return;
        }
        int[] pick = pairs.get(rnd.nextInt(pairs.size()));
        String word = WORDS[pick[0]];
        String correct = WORDS[pick[1]];
        char last = word.charAt(word.length() - 1);
        List<String> pool = new ArrayList<>();
        for (String w : WORDS) {
            if (!w.equals(correct) && w.charAt(0) != last && !pool.contains(w)) pool.add(w);
        }
        List<String> opts = new ArrayList<>();
        int ans = shuffleOpts(opts, correct, pool, 4, rnd);
        setQ(word + "  →  ?（接尾字母）", opts.toArray(new String[0]), ans);
    }
}
