package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.ArcShotGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g072 投篮大师：拖拽蓄力抛物投篮，篮筐左右移动；
 *  命中连击加倍，三轮越投越远。 */
public class BasketShotGame extends ArcShotGame {
    private final Random rnd = new Random();
    private float hoopDir = 1;
    private int combo;

    public BasketShotGame() {
        super();
    }

    @Override
    protected float gravity() {
        return 0.16f;
    }

    @Override
    protected int shotsPerRound() {
        return 6;
    }

    @Override
    protected int totalRounds() {
        return 3;
    }

    @Override
    protected void buildTargets(int round) {
        targets.clear();
        float x = 200 + round * 30 + rnd.nextInt(40);
        float y = 170 - round * 20;
        targets.add(new float[]{Math.min(x, lw - 60), y, 30, hoopDir * (1f + round * 0.3f)});
    }

    @Override
    protected int hitScore(float[] t) {
        combo++;
        return 50 * Math.min(4, combo);
    }

    @Override
    protected void onShotMissed() {
        combo = 0;
    }

    @Override
    protected void onTick() {
        super.onTick();
        // 篮筐漂移
        for (float[] t : targets) {
            t[0] += t[3];
            if (t[0] < 160 || t[0] > lw - 70) t[3] = -t[3];
        }
    }

    @Override
    protected void drawTarget(Canvas c, float[] t) {
        // 篮板 + 筐
        fill.setColor(0xFFC9B896);
        rr(c, t[0] + 22, t[1] - 40, t[0] + 30, t[1] + 30, 2, 0xFFC9B896);
        stroke.setColor(0xFFE8574F);
        stroke.setStrokeWidth(4);
        c.drawCircle(t[0], t[1], 22, stroke);
        stroke.setColor(0xFFFFFFFF);
        stroke.setStrokeWidth(2);
        c.drawLine(t[0] - 22, t[1], t[0] + 22, t[1], stroke);
        label(c, "连击 ×" + Math.max(1, combo), lw / 2f, 60, 14, 0xFFFFC24B, true);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF2A2018);
        rr(c, 0, 520, lw, lh - 520, 0, 0xFF8A5A2B);
        stroke.setColor(0x55FFFFFF);
        stroke.setStrokeWidth(1);
        c.drawLine(0, 470, lw, 470, stroke);
    }
}
