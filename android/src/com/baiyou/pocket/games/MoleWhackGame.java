package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.PopTargetGame;

/** g028 地鼠快打：九宫地洞里冒地鼠（+10）和炸弹（点了扣命），
 *  地鼠自己缩回去不扣命。45 秒看谁敲得多。 */
public class MoleWhackGame extends PopTargetGame {
    private static final int HOLES = 3;
    private static final float GRID_LEFT = 55, GRID_TOP = 150, HOLE = 80;

    public MoleWhackGame() {
        super();
    }

    @Override
    protected long spawnGapMs() {
        return Math.max(380, 800 - score * 4);
    }

    @Override
    protected long lifeMs() {
        return 950;
    }

    @Override
    protected int maxOnScreen() {
        return 3;
    }

    @Override
    protected long roundMs() {
        return 45000;
    }

    @Override
    protected boolean missCostsLife() {
        return false; // 地鼠缩回去不扣命
    }

    @Override
    protected int nextKind() {
        return randi(100) < 78 ? KIND_TARGET : KIND_BOMB;
    }

    @Override
    protected void spawnOne() {
        // 只在洞口生成
        int h = randi(HOLES * HOLES);
        float x = GRID_LEFT + (h % HOLES) * (HOLE + 20) + HOLE / 2f;
        float y = GRID_TOP + (h / HOLES) * (HOLE + 22) + HOLE / 2f;
        spawnEnt(x, y, 30, lifeMs(), nextKind());
    }

    @Override
    protected int hitScore(int kind) {
        return 10;
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        long age = now() - (long) e[3];
        float k = Math.min(1f, age / 120f);
        float r = e[2] * (0.4f + 0.6f * easeOut(k));
        boolean bomb = (int) e[5] == KIND_BOMB;
        fill.setColor(bomb ? 0xFF3A3F4C : 0xFF8A5A2B);
        c.drawCircle(e[0], e[1], r, fill);
        // 眼睛/引信
        if (bomb) {
            label(c, "✸", e[0], e[1], r * 1.4f, 0xFFFFC24B, true);
        } else {
            fill.setColor(0xFF1B1C22);
            c.drawCircle(e[0] - r * 0.3f, e[1] - r * 0.15f, r * 0.14f, fill);
            c.drawCircle(e[0] + r * 0.3f, e[1] - r * 0.15f, r * 0.14f, fill);
            fill.setColor(0xFFE8A87C);
            c.drawCircle(e[0], e[1] + r * 0.3f, r * 0.2f, fill);
        }
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E2B1E);
        for (int h = 0; h < HOLES * HOLES; h++) {
            float x = GRID_LEFT + (h % HOLES) * (HOLE + 20);
            float y = GRID_TOP + (h / HOLES) * (HOLE + 22);
            fill.setColor(0xFF142014);
            c.drawCircle(x + HOLE / 2f, y + HOLE / 2f, HOLE / 2f - 4, fill);
        }
        label(c, "敲地鼠 · 躲炸弹", lw / 2f, 40, 13, 0xFF9ACDAA, false);
    }
}
