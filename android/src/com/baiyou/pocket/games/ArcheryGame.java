package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.ArcShotGame;

import java.util.Random;

/** g079 射箭小屋：拖拽射箭，箭受重力与风偏影响，靶子远近轮换；
 *  命中环数即得分。 */
public class ArcheryGame extends ArcShotGame {
    private final Random rnd = new Random();

    public ArcheryGame() {
        super();
    }

    @Override
    protected float gravity() {
        return 0.11f;
    }

    @Override
    protected float powerScale() {
        return 0.085f;
    }

    @Override
    protected int shotsPerRound() {
        return 5;
    }

    @Override
    protected int totalRounds() {
        return 3;
    }

    @Override
    protected void buildTargets(int round) {
        targets.clear();
        float x = 230 + round * 24 + rnd.nextInt(50);
        float y = 170 + rnd.nextInt(160);
        targets.add(new float[]{Math.min(x, lw - 50), y, 26 + rnd.nextInt(16), 0});
    }

    @Override
    protected int hitScore(float[] t) {
        // 越小越远的靶分越高
        return Math.round(160 - t[2] * 2.4f);
    }

    @Override
    protected void drawTarget(Canvas c, float[] t) {
        fill.setColor(0xFFF4F5F9);
        c.drawCircle(t[0], t[1], t[2], fill);
        fill.setColor(0xFFE8574F);
        c.drawCircle(t[0], t[1], t[2] * 0.62f, fill);
        fill.setColor(0xFFFFC24B);
        c.drawCircle(t[0], t[1], t[2] * 0.28f, fill);
    }

    @Override
    protected void drawLauncherDecor(Canvas c) {
        // 弓
        stroke.setColor(0xFF8A5A2B);
        stroke.setStrokeWidth(4);
        rf.set(launcherX - 26, launcherY - 26, launcherX + 26, launcherY + 26);
        c.drawArc(rf, -60, 120, false, stroke);
        stroke.setColor(0xFFECECF1);
        stroke.setStrokeWidth(1.5f);
        c.drawLine(launcherX + 8, launcherY - 22, launcherX + 8, launcherY + 22, stroke);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF3A5C8A);
        rr(c, 0, 470, lw, lh - 470, 0, 0xFF2E7C4A);
        fill.setColor(0xFF2E7C4A);
        for (int i = 0; i < 5; i++) {
            float tx = 40 + i * 80;
            Path p = new Path();
            p.moveTo(tx, 470);
            p.lineTo(tx + 8, 430);
            p.lineTo(tx - 8, 430);
            p.close();
            c.drawPath(p, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
