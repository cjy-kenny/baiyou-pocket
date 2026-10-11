package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g096 拼字大师：看一眼词语，然后把打乱的字块按顺序点回来；
 *  点错顺序立即重排并扣命，8 个词连拼。 */
public class WordBuildGame extends CanvasGame {
    private static final String[] WORDS = {
            "水果", "阳光", "奔跑", "微笑", "森林", "星空", "海洋", "月光",
            "雨伞", "花园", "面包", "火车", "风筝", "铅笔", "书包", "台灯",
    };
    private static final int TOTAL = 8;

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();
    private String word;
    private final List<String> tiles = new ArrayList<>();
    private final List<Integer> picked = new ArrayList<>(); // 已点块下标
    private int round, lives = 3, shownScore;
    private boolean showing, settling;
    private long showUntil;
    private boolean settledAll;

    public WordBuildGame() {
        super(360, 560);
        nextRound();
    }

    private void nextRound() {
        round++;
        if (used.size() >= WORDS.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(WORDS.length);
        } while (used.contains(i));
        used.add(i);
        word = WORDS[i];
        tiles.clear();
        picked.clear();
        for (int k = 0; k < word.length(); k++) tiles.add(String.valueOf(word.charAt(k)));
        java.util.Collections.shuffle(tiles, rnd);
        // 防止恰好原序
        boolean same = true;
        for (int k = 0; k < tiles.size(); k++)
            if (!tiles.get(k).equals(String.valueOf(word.charAt(k)))) same = false;
        if (same && tiles.size() > 1) {
            String t = tiles.remove(0);
            tiles.add(t);
        }
        showing = true;
        showUntil = now() + 1100;
    }

    private void startGame() {
        round = 0;
        lives = 3;
        shownScore = 0;
        settling = false;
        over = false;
        nextRound();
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || showing || action != 0) return;
        for (int i = 0; i < tiles.size(); i++) {
            if (picked.contains(i)) continue;
            float tx = 40 + i * 72;
            if (hit(x, y, tx, 330, 64, 72)) {
                picked.add(i);
                String built = built();
                if (built.length() == word.length()) {
                    if (built.equals(word)) {
                        shownScore += 120;
                        host.setScore(shownScore);
                        beep(1);
                        burst(180, 366, 0xFF7CFFB2, 12, 5f);
                        if (round >= TOTAL) {
                            over = true;
                            stopLoop();
                            popup("连拼八词！", lw / 2f, lh / 2f, 0xFFFFC24B, 20);
                            dirty();
                            postDelayedSafe(() -> host.gameOver(shownScore), 900);
                            return;
                        }
                        settling = true;
                        postDelayedSafe(() -> {
                            settling = false;
                            if (over) return;
                            nextRound();
                            dirty();
                        }, 550);
                    } else {
                        lives--;
                        beep(2);
                        shake(220);
                        picked.clear();
                        if (lives <= 0) {
                            over = true;
                            stopLoop();
                            popup("第 " + round + " 词拼错了", lw / 2f, lh / 2f, 0xFFE8574F, 17);
                            int finalScore = (round - 1) * 120;
                            host.setScore(finalScore);
                            dirty();
                            postDelayedSafe(() -> host.gameOver(finalScore), 850);
                            return;
                        }
                    }
                } else {
                    beep(0);
                }
                dirty();
                return;
            }
        }
    }

    private String built() {
        StringBuilder sb = new StringBuilder();
        for (int i : picked) sb.append(tiles.get(i));
        return sb.toString();
    }

    @Override
    protected void onTick() {
        if (showing && now() >= showUntil) showing = false;
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF26221C);
        label(c, "第 " + Math.min(round, TOTAL) + "/" + TOTAL + " 词",
                lw / 2f, 90, 14, 0xFFC9B896, false);
        if (showing) {
            label(c, "记住这个词", lw / 2f, 210, 15, 0xFFFFC24B, true);
            label(c, word, lw / 2f, 260, 38, 0xFFECECF1, true);
        } else {
            label(c, "按顺序点回来", lw / 2f, 180, 15, 0xFFECECF1, true);
            // 已拼部分
            String built = built();
            label(c, built.isEmpty() ? "＿" : built, lw / 2f, 240, 30, 0xFF7CFFB2, true);
            // 字块
            for (int i = 0; i < tiles.size(); i++) {
                float tx = 40 + i * 72;
                boolean gone = picked.contains(i);
                rr(c, tx, 330, tx + 64, 402, 10, gone ? 0xFF2A2520 : 0xFF3E362A);
                if (!gone) {
                    label(c, tiles.get(i), tx + 32, 366, 28, 0xFFECECF1, true);
                }
            }
        }
        hearts(c, lives, 3, lw - 24, 30, 8);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
