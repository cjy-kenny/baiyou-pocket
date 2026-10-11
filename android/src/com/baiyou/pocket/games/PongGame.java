package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g075 乒乓速对：球拍随手指横移，与 AI 来回拉锯，先得 5 分者胜。 */
public class PongGame extends CanvasGame {
    private static final float PW = 74, PH = 12;

    private final Random rnd = new Random();
    private float px = 180;   // 玩家拍
    private float ax = 180;   // AI 拍
    private float bx = 180, by = 300, bvx = 3.4f, bvy = 4.2f;
    private int me, ai;
    private boolean serving;
    private long serveUntil;
    private boolean settling;

    public PongGame() {
        super(360, 560);
        serve();
    }

    private void serve() {
        bx = 180;
        by = 300;
        bvx = rnd.nextBoolean() ? 3.4f : -3.4f;
        bvy = (rnd.nextBoolean() ? 1 : -1) * 4.4f;
        serving = true;
        serveUntil = now() + 700;
    }

    private void startRound() {
        me = 0;
        ai = 0;
        settling = false;
        over = false;
        host.setScore(0);
        serve();
        startLoop(16);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over) return;
        px = Math.max(PW / 2, Math.min(lw - PW / 2, x));
        dirty();
    }

    private void point(boolean meScores) {
        if (meScores) {
            me++;
            host.setScore(me * 100);
            beep(1);
            popup("得分！", lw / 2f, lh / 2f, 0xFF7CFFB2, 18);
        } else {
            ai++;
            beep(2);
            popup("失分", lw / 2f, lh / 2f, 0xFFE8574F, 18);
        }
        if (me >= 5 || ai >= 5) {
            settling = true;
            over = true;
            stopLoop();
            boolean win = me > ai;
            host.setScore(me * 100 + (win ? 300 : 0));
            popup(win ? "你赢了！" : "惜败", lw / 2f, lh / 2f - 40, win ? 0xFFFFC24B : 0xFFECECF1, 22);
            dirty();
            postDelayedSafe(() -> host.gameOver(me * 100 + (win ? 300 : 0)), 950);
            return;
        }
        serve();
    }

    @Override
    protected void onTick() {
        if (serving) {
            if (now() >= serveUntil) serving = false;
            dirty();
            return;
        }
        bx += bvx;
        by += bvy;
        // 墙
        if (bx < 16 || bx > lw - 16) {
            bvx = -bvx;
            bx = Math.max(16, Math.min(lw - 16, bx));
            beep(0);
        }
        // AI 拍
        float want = bx + (float) Math.sin(now() / 400.0) * 30;
        ax += (want - ax) * 0.08f;
        ax = Math.max(PW / 2, Math.min(lw - PW / 2, ax));
        // AI 击球
        if (bvy < 0 && by < 60 && by > 44 && Math.abs(bx - ax) < PW / 2 + 8) {
            bvy = Math.abs(bvy);
            bvx += (bx - ax) * 0.12f;
            bvx = Math.max(-6f, Math.min(6f, bvx));
            beep(0);
        }
        // 玩家击球
        if (bvy > 0 && by > 500 && by < 522 && Math.abs(bx - px) < PW / 2 + 8) {
            bvy = -Math.abs(bvy) * 1.03f;
            bvx += (bx - px) * 0.12f;
            bvx = Math.max(-6.5f, Math.min(6.5f, bvx));
            beep(0);
            burst(bx, by, 0xFF7CFFB2, 6, 3f);
        }
        // 出界
        if (by > 570) {
            point(false);
            return;
        }
        if (by < -20) {
            point(true);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF182430);
        c.drawLine(0, lh / 2f, lw, lh / 2f, stroke);
        // AI 拍
        rr(c, ax - PW / 2, 48, ax + PW / 2, 48 + PH, 5, 0xFFE8848C);
        // 玩家拍
        rr(c, px - PW / 2, 505, px + PW / 2, 505 + PH, 5, 0xFF7CFFB2);
        // 球
        fill.setColor(0xFFFFC24B);
        c.drawCircle(bx, by, 9, fill);
        label(c, me + " : " + ai, lw / 2f, lh / 2f - 14, 22, 0xFFECECF1, true);
        if (serving) label(c, "准备…", lw / 2f, lh / 2f + 20, 14, 0xFF8B93A7, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
