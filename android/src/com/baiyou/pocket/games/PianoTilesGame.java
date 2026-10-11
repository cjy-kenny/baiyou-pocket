package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g027 黑块快弹：四轨黑块不断下落，黑块进入底部击打区时点它；
 *  点到白轨或漏掉黑块即结束，弹过的块数就是分。 */
public class PianoTilesGame extends CanvasGame {
    private static final int TRACKS = 4, TRACK_W = 82, LEFT = 16, TOP = 70;
    private static final float HIT_Y = 470;

    private static class Tile {
        float y;
        int track;
        boolean hit;

        Tile(int track, float y) {
            this.track = track;
            this.y = y;
        }
    }

    private final List<Tile> tiles = new ArrayList<>();
    private final Random rnd = new Random();
    private float speed = 3.2f;
    private int lastSpawnTrack = -1, score;
    private boolean dying;

    public PianoTilesGame() {
        super(360, 560);
        for (int i = 0; i < 5; i++) spawnTile(TOP - i * 130);
    }

    private void spawnTile(float y) {
        int t = rnd.nextInt(TRACKS);
        if (t == lastSpawnTrack && rnd.nextInt(3) == 0) t = (t + 1 + rnd.nextInt(TRACKS - 1)) % TRACKS;
        lastSpawnTrack = t;
        tiles.add(new Tile(t, y));
    }

    private void startRound() {
        tiles.clear();
        speed = 3.2f;
        score = 0;
        for (int i = 0; i < 5; i++) spawnTile(TOP - i * 130);
        dying = false;
        over = false;
        host.setScore(0);
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

    private float trackX(int t) {
        return LEFT + t * TRACK_W;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying || action != 0) return;
        int t = (int) ((x - LEFT) / TRACK_W);
        if (t < 0 || t >= TRACKS) return;
        // 找该轨最靠近击打区的未击中黑块
        Tile best = null;
        for (Tile tile : tiles) {
            if (tile.track != t || tile.hit) continue;
            if (best == null || tile.y > best.y) best = tile;
        }
        if (best != null && best.y > HIT_Y - 130 && best.y < HIT_Y + 60) {
            best.hit = true;
            score++;
            speed = Math.min(7.5f, speed + 0.12f);
            host.setScore(score);
            beep(score % 4 == 0 ? 3 : 1);
            burst(trackX(t) + TRACK_W / 2f, best.y, 0xFF7CFFB2, 8, 4f);
        } else {
            die(t);
        }
        dirty();
    }

    private void die(int t) {
        dying = true;
        over = true;
        stopLoop();
        shake(300);
        burst(trackX(t) + TRACK_W / 2f, HIT_Y, 0xFFE8574F, 16, 6f);
        beep(2);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 750);
    }

    @Override
    protected void onTick() {
        Iterator<Tile> it = tiles.iterator();
        while (it.hasNext()) {
            Tile tile = it.next();
            if (tile.hit) {
                tile.y += speed * 2.5f; // 已击中的飞出
            } else {
                tile.y += speed;
            }
            if (tile.y > lh + 60) it.remove();
            if (!tile.hit && tile.y > HIT_Y + 70) {
                die(tile.track);
                return;
            }
        }
        float lowest = 0;
        for (Tile tile : tiles) lowest = Math.max(lowest, tile.y);
        if (lowest < TOP + 40) spawnTile(Math.min(TOP, lowest) - 130);
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20242E);
        label(c, String.valueOf(score), lw / 2f, 40, 30, 0xFFECECF1, true);
        for (int t = 0; t < TRACKS; t++) {
            rr(c, trackX(t) + 2, TOP, trackX(t) + TRACK_W - 2, lh, 3, 0xFF2A2E38);
            // 击打区
            stroke.setColor(0x667CFFB2);
            stroke.setStrokeWidth(2);
            rf.set(trackX(t) + 2, HIT_Y - 60, trackX(t) + TRACK_W - 2, HIT_Y + 60);
            c.drawRoundRect(rf, 8, 8, stroke);
        }
        for (Tile tile : tiles) {
            if (tile.hit) continue;
            rr(c, trackX(tile.track) + 6, tile.y, trackX(tile.track) + TRACK_W - 6, tile.y + 110,
                    6, 0xFF101318);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
