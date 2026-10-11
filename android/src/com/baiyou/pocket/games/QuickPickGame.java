package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g033 眼疾手快：顶部给出目标色，盘面出一堆色块，只点目标色的块；
 *  点错或漏掉消失扣命，30 秒冲分。 */
public class QuickPickGame extends CanvasGame {
    private static final long ROUND_MS = 30000;
    private static final int[] COLORS = {
            0xFFE8574F, 0xFFF2A33C, 0xFF2FA36B, 0xFF4A90E2, 0xFF9B59F6};
    private static final int GRID = 4;
    private static final float CELL = 68, LEFT = 34, TOP = 120;

    private final Random rnd = new Random();
    private int target;
    private final int[] kinds = new int[GRID * GRID];
    private final boolean[] gone = new boolean[GRID * GRID];
    private int lives = 3, shownScore;
    private long roundEnd, cellUntil;
    private boolean settling;

    public QuickPickGame() {
        super(360, 560);
        nextWave();
    }

    private void nextWave() {
        target = rnd.nextInt(COLORS.length);
        for (boolean b : gone) b = false;
        for (int i = 0; i < kinds.length; i++) kinds[i] = rnd.nextInt(COLORS.length);
        kinds[rnd.nextInt(kinds.length)] = target; // 保证至少一个
        cellUntil = now() + Math.max(900, 1900 - shownScore / 10);
    }

    private void startRound() {
        lives = 3;
        shownScore = 0;
        roundEnd = now() + ROUND_MS;
        nextWave();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int c = (int) ((x - LEFT) / CELL), r = (int) ((y - TOP) / CELL);
        if (r < 0 || r >= GRID || c < 0 || c >= GRID || gone[r * GRID + c]) return;
        int i = r * GRID + c;
        if (kinds[i] == target) {
            gone[i] = true;
            shownScore += 10;
            host.setScore(shownScore);
            beep(1);
            burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, COLORS[target], 8, 4f);
            if (allGone()) nextWave();
        } else {
            lives--;
            beep(2);
            shake(180);
            if (lives <= 0) {
                settling = true;
                over = true;
                stopLoop();
                popup("手滑了", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 850);
                return;
            }
        }
        dirty();
    }

    private boolean allGone() {
        for (boolean b : gone) if (!b) return false;
        return true;
    }

    @Override
    protected void onTick() {
        long now = now();
        if (now >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        if (now >= cellUntil) {
            // 本波超时：全部换新并扣命
            lives--;
            beep(2);
            if (lives <= 0) {
                over = true;
                stopLoop();
                popup("慢了一步", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 850);
                return;
            }
            nextWave();
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E222B);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "只点这种颜色：", 30, 95, 13, 0xFF8B8E9C, false);
        fill.setColor(COLORS[target]);
        c.drawCircle(lw - 60, 90, 18, fill);
        for (int r = 0; r < GRID; r++)
            for (int col = 0; col < GRID; col++) {
                int i = r * GRID + col;
                if (gone[i]) continue;
                float l = cellX(col), t = cellY(r);
                rr(c, l + 4, t + 4, l + CELL - 4, t + CELL - 4, 10, COLORS[kinds[i]]);
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private float cellX(int c) {
        return LEFT + c * CELL;
    }

    private float cellY(int r) {
        return TOP + r * CELL;
    }
}
