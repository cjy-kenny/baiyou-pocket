package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/** g020 汉诺塔影：三根柱，点柱拿起/放下圆盘（大盘不能压小盘），
 *  全部移到右柱过关；越接近最优步数分越高。 */
public class HanoiGame extends CanvasGame {
    private final Deque<Integer>[] pegs = new Deque[3];
    private int disks = 4, selected = -1, moves;
    private boolean settling;

    public HanoiGame() {
        super(360, 560);
        startRound();
    }

    private void startRound() {
        for (int i = 0; i < 3; i++) pegs[i] = new ArrayDeque<>();
        for (int d = disks; d >= 1; d--) pegs[0].addLast(d);
        selected = -1;
        moves = 0;
        settling = false;
        over = false;
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
        host.setScore(0);
    }

    private float pegX(int i) {
        return 60 + i * 120;
    }

    private static final float PEG_TOP = 190, PEG_BASE = 470, DISK_H = 22;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int p = -1;
        for (int i = 0; i < 3; i++)
            if (hit(x, y, pegX(i) - 55, PEG_TOP - 60, 110, PEG_BASE - PEG_TOP + 80)) p = i;
        if (p < 0) return;
        if (selected < 0) {
            if (!pegs[p].isEmpty()) {
                selected = p;
                beep(0);
            }
        } else if (p == selected) {
            selected = -1;
            beep(0);
        } else {
            Integer top = pegs[p].peekLast();
            Integer moving = pegs[selected].peekLast();
            if (top != null && moving != null && top < moving) {
                beep(2);
                popup("大盘不能压小盘", pegX(p), PEG_TOP - 30, 0xFFE8574F, 13);
            } else {
                pegs[p].addLast(pegs[selected].removeLast());
                moves++;
                beep(1);
                if (pegs[2].size() == disks) {
                    settleWin();
                    return;
                }
            }
            selected = -1;
        }
        dirty();
    }

    private void settleWin() {
        settling = true;
        int optimal = (1 << disks) - 1;
        int finalScore = Math.max(100, 1000 - Math.max(0, moves - optimal) * 25) + disks * 100;
        host.setScore(finalScore);
        popup("完成！ " + finalScore + " 分", lw / 2f, 140, 0xFFFFC24B, 20);
        burst(pegX(2), PEG_BASE - 30, 0xFFFFC24B, 18, 7f);
        beep(3);
        dirty();
        postDelayedSafe(() -> {
            settling = false;
            if (disks >= 5) {
                over = true;
                stopLoop();
                popup("全部通关", lw / 2f, 110, 0xFFECECF1, 16);
                dirty();
                postDelayedSafe(() -> host.gameOver(finalScore), 700);
            } else {
                disks++;
                startRoundKeepScore(finalScore);
            }
        }, 1000);
    }

    private void startRoundKeepScore(int acc) {
        for (int i = 0; i < 3; i++) pegs[i] = new ArrayDeque<>();
        for (int d = disks; d >= 1; d--) pegs[0].addLast(d);
        selected = -1;
        moves = 0;
        host.setScore(acc);
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF191722);
        label(c, moves + " 步 · 最优 " + ((1 << disks) - 1) + " 步 · " + disks + " 盘",
                lw / 2f, 60, 13, 0xFFB9AED0, false);
        // 柱与底盘
        for (int i = 0; i < 3; i++) {
            rr(c, pegX(i) - 4, PEG_TOP, pegX(i) + 4, PEG_BASE, 3, 0xFF6B5E8F);
        }
        rr(c, 30, PEG_BASE, lw - 30, PEG_BASE + 14, 6, 0xFF3A3352);
        for (int i = 0; i < 3; i++) {
            int slot = 0;
            Iterator<Integer> it = pegs[i].iterator();
            while (it.hasNext()) {
                int d = it.next();
                float w = 40 + d * 16;
                float y = PEG_BASE - (slot + 1) * DISK_H;
                int color = 0xFF000000
                        | ((0x5B + d * 28) << 16) | ((0x8D + d * 6) << 8) | (0xF0 - d * 9);
                rr(c, pegX(i) - w / 2f, y, pegX(i) + w / 2f, y + DISK_H - 3, 5, color);
                slot++;
            }
            // 拿起中的盘悬浮显示
            if (selected == i && !pegs[i].isEmpty()) {
                int d = pegs[i].peekLast();
                float w = 40 + d * 16;
                rr(c, pegX(i) - w / 2f, PEG_TOP - 50, pegX(i) + w / 2f, PEG_TOP - 50 + DISK_H - 3,
                        5, 0xFFFFC24B);
            }
        }
        label(c, "点柱拿盘 · 再点柱放下", lw / 2f, lh - 18, 11, 0xFF8B8E9C, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
