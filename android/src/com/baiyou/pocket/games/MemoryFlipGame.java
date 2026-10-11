package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g003 记忆翻翻乐：4×5 共 20 张牌、10 对符号。
 *  开局 1.4 秒预览全部牌面；卡牌翻转有动画；每多翻一步 -30、每秒 -2，
 *  满分 1200（下限 100）；全部配对即结算。 */
public class MemoryFlipGame extends CanvasGame {
    private static final int COLS = 4, ROWS = 5;
    private static final int CW = 78, CH = 98, GX = 10, GY = 10, PAD = 16, HINT = 36;
    private static final long FLIP_MS = 200, PREVIEW_MS = 1400, REVEAL_MS = 750;

    private final int[] ids = new int[COLS * ROWS];   // 0-9 成对
    private final boolean[] flipped = new boolean[COLS * ROWS];
    private final boolean[] matched = new boolean[COLS * ROWS];
    /** 翻转动画：{idx, start, fromFace(1/0), toFace(1/0)} */
    private final List<float[]> flips = new ArrayList<>();
    private final Random rnd = new Random();
    private int first = -1, second = -1;
    private long lockUntil, previewUntil;
    private boolean previewWasActive;
    private int moves, matchedCount;
    private long startAt;

    public MemoryFlipGame() {
        super(16 * 2 + COLS * CW + (COLS - 1) * GX,
                16 * 2 + ROWS * CH + (ROWS - 1) * GY + HINT);
        shuffle();
    }

    private void shuffle() {
        for (int i = 0; i < ids.length; i++) ids[i] = i / 2;   // 0,0,1,1,...
        for (int i = ids.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = ids[i];
            ids[i] = ids[j];
            ids[j] = t;
        }
    }

    private void startRound() {
        shuffle();
        for (int i = 0; i < ids.length; i++) {
            flipped[i] = false;
            matched[i] = false;
        }
        flips.clear();
        first = second = -1;
        lockUntil = 0;
        moves = 0;
        matchedCount = 0;
        startAt = 0;
        over = false;
        previewUntil = System.currentTimeMillis() + PREVIEW_MS;
        previewWasActive = true;
        host.setScore(0);
        startLoop(80);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(80);
    }

    private long elapsedSec() {
        return startAt == 0 ? 0 : (System.currentTimeMillis() - startAt) / 1000;
    }

    private int currentScore() {
        int s = 1200 - Math.max(0, moves - 10) * 30 - (int) elapsedSec() * 2;
        return Math.max(100, s);
    }

    private float cardL(int i) {
        return PAD + (i % COLS) * (CW + GX);
    }

    private float cardT(int i) {
        return PAD + (i / COLS) * (CH + GY);
    }

    // ---------------- 交互 ----------------

    @Override
    protected void touch(float x, float y, int action) {
        if (over || action != 0 || lockUntil > 0 || System.currentTimeMillis() < previewUntil) return;
        int idx = -1;
        for (int i = 0; i < ids.length; i++) {
            float l = cardL(i), t = cardT(i);
            if (x >= l && x <= l + CW && y >= t && y <= t + CH) {
                idx = i;
                break;
            }
        }
        if (idx < 0 || matched[idx] || flipped[idx]) return;
        if (startAt == 0) startAt = System.currentTimeMillis();
        flipped[idx] = true;
        flips.add(new float[]{idx, System.currentTimeMillis(), 0, 1}); // 背→面
        beep(0);
        if (first < 0) {
            first = idx;
        } else {
            moves++;
            if (ids[first] == ids[idx]) {
                matched[first] = true;
                matched[idx] = true;
                first = second = -1;
                matchedCount += 2;
                beep(1);
                // 配对特效
                burst(cardL(idx) + CW / 2f, cardT(idx) + CH / 2f, 0xFF7CFFB2, 10, 4.5f);
                burst(cardL(idx) + CW / 2f, cardT(idx) + CH / 2f, 0xFF5B8DEF, 8, 4.5f);
                if (matchedCount == ids.length) {
                    over = true;
                    stopLoop();
                    beep(3);
                    popup("全部配对！", lw / 2f, lh / 2f, 0xFFFFC24B, 24);
                    host.gameOver(currentScore());
                } else {
                    popup("配对成功！", cardL(idx) + CW / 2f, cardT(idx) + CH / 2f - 8,
                            0xFF7CFFB2, 15);
                    host.setScore(currentScore());
                }
            } else {
                second = idx;
                lockUntil = System.currentTimeMillis() + REVEAL_MS;
            }
        }
        dirty();
    }

    // ---------------- 逻辑帧：错配翻回 + 动画清理 ----------------

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        // 预览结束：整版卡牌统一翻回背面
        if (previewWasActive && now >= previewUntil) {
            previewWasActive = false;
            for (int i = 0; i < ids.length; i++) flips.add(new float[]{i, now, 1, 0});
            beep(0);
        }
        if (lockUntil > 0 && now >= lockUntil) {
            if (first >= 0) {
                flipped[first] = false;
                flips.add(new float[]{first, now, 1, 0}); // 面→背
            }
            if (second >= 0) {
                flipped[second] = false;
                flips.add(new float[]{second, now, 1, 0});
            }
            first = second = -1;
            lockUntil = 0;
        }
        boolean animating = false;
        Iterator<float[]> it = flips.iterator();
        while (it.hasNext()) {
            float[] f = it.next();
            if (now - f[1] >= FLIP_MS) it.remove();
            else animating = true;
        }
        dirty();
    }

    // ---------------- 渲染 ----------------

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF232733);
        long now = System.currentTimeMillis();
        boolean preview = now < previewUntil;

        for (int i = 0; i < ids.length; i++) {
            float l = cardL(i), t = cardT(i);
            // 翻转动画：前半程收缩显示"来源面"，后半程展开显示"目标面"
            float k = -1f; // -1 = 无动画
            boolean fromFace = false;
            for (float[] f : flips) {
                if ((int) f[0] == i) {
                    k = (now - f[1]) / (float) FLIP_MS;
                    fromFace = f[2] > 0;
                    break;
                }
            }
            boolean face = preview || flipped[i] || matched[i];
            if (k < 0) {
                drawCard(c, i, l, t, face ? 1f : 0f, 1f);
            } else if (k < 0.5f) {
                drawCard(c, i, l, t, fromFace ? 1f : 0f, 1f - k * 2);
            } else {
                drawCard(c, i, l, t, face ? 1f : 0f, (k - 0.5f) * 2);
            }
        }
        label(c, preview ? "记住所有牌面…" : "已翻 " + moves + " 步 · 少翻多想",
                lw / 2f, lh - 16, 12, 0xFF8B8E9C, false);
    }

    /** faceK：0=背面 1=正面（用于翻转插值）；scaleK：横向展开比例。 */
    private void drawCard(Canvas c, int i, float l, float t, float faceK, float scaleK) {
        float cx = l + CW / 2f, w = CW * Math.max(0.06f, scaleK);
        RectF rf = new RectF(cx - w / 2f, t, cx + w / 2f, t + CH);
        if (faceK < 0.5f) {
            // 背面
            fill.setColor(0xFF4C566F);
            c.drawRoundRect(rf, 10, 10, fill);
            if (scaleK > 0.5f) label(c, "?", cx, t + CH / 2f, 34, 0xFF8B93A7, true);
        } else if (matched[i]) {
            fill.setColor(0xFF3A4152);
            c.drawRoundRect(rf, 10, 10, fill);
            if (scaleK > 0.5f) drawSymbol(c, ids[i], cx, t + CH / 2f, 0x88FFFFFF);
        } else {
            fill.setColor(0xFFF4F5F9);
            c.drawRoundRect(rf, 10, 10, fill);
            if (scaleK > 0.5f) drawSymbol(c, ids[i], cx, t + CH / 2f, 0xFF33363F);
        }
    }

    /** 10 种符号（形状 + 专属颜色，全部程序化绘制）。 */
    private void drawSymbol(Canvas c, int id, float cx, float cy, int color) {
        fill.setColor(color);
        stroke.setColor(color);
        stroke.setStrokeWidth(5);
        float r = 22;
        switch (id) {
            case 0: // 圆
                c.drawCircle(cx, cy, r, fill);
                break;
            case 1: // 方
                rr(c, cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.85f, cy + r * 0.85f, 8, color);
                break;
            case 2: // 三角
                tri(c, cx, cy, r * 1.1f, color);
                break;
            case 3: // 菱形
                Path d = new Path();
                d.moveTo(cx, cy - r * 1.15f);
                d.lineTo(cx + r * 0.85f, cy);
                d.lineTo(cx, cy + r * 1.15f);
                d.lineTo(cx - r * 0.85f, cy);
                d.close();
                c.drawPath(d, fill);
                break;
            case 4: // 五角星
                star(c, cx, cy, r * 1.25f, r * 0.52f, 5, color);
                break;
            case 5: // 十字
                float a = r * 0.36f, b = r * 1.05f;
                rr(c, cx - a, cy - b, cx + a, cy + b, 6, color);
                rr(c, cx - b, cy - a, cx + b, cy + a, 6, color);
                break;
            case 6: // 圆环
                fill.setStyle(Paint.Style.STROKE);
                fill.setStrokeWidth(9);
                c.drawCircle(cx, cy, r * 0.8f, fill);
                fill.setStyle(Paint.Style.FILL);
                break;
            case 7: // 六边形
                Path h = new Path();
                for (int k = 0; k < 6; k++) {
                    double ang = Math.PI / 3 * k - Math.PI / 2;
                    float px = cx + (float) Math.cos(ang) * r;
                    float py = cy + (float) Math.sin(ang) * r;
                    if (k == 0) h.moveTo(px, py);
                    else h.lineTo(px, py);
                }
                h.close();
                c.drawPath(h, fill);
                break;
            case 8: // 月亮
                Path m = new Path();
                m.addCircle(cx, cy, r * 0.95f, Path.Direction.CW);
                m.addCircle(cx + r * 0.5f, cy - r * 0.28f, r * 0.78f, Path.Direction.CW);
                m.setFillType(Path.FillType.EVEN_ODD);
                c.drawPath(m, fill);
                break;
            default: // 心形
                Path ht = new Path();
                ht.moveTo(cx, cy + r);
                ht.cubicTo(cx - r * 1.5f, cy - r * 0.2f, cx - r * 0.6f, cy - r * 1.3f, cx, cy - r * 0.45f);
                ht.cubicTo(cx + r * 0.6f, cy - r * 1.3f, cx + r * 1.5f, cy - r * 0.2f, cx, cy + r);
                c.drawPath(ht, fill);
                break;
        }
    }

    private void tri(Canvas c, float cx, float cy, float w, int color) {
        Path p = new Path();
        p.moveTo(cx, cy - w * 0.62f);
        p.lineTo(cx + w * 0.55f, cy + w * 0.4f);
        p.lineTo(cx - w * 0.55f, cy + w * 0.4f);
        p.close();
        fill.setColor(color);
        c.drawPath(p, fill);
    }

    private void star(Canvas c, float cx, float cy, float ro, float ri, int n, int color) {
        Path p = new Path();
        for (int k = 0; k < n * 2; k++) {
            double ang = Math.PI / n * k - Math.PI / 2;
            float rad = (k % 2 == 0) ? ro : ri;
            float px = cx + (float) Math.cos(ang) * rad;
            float py = cy + (float) Math.sin(ang) * rad;
            if (k == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        p.close();
        fill.setColor(color);
        c.drawPath(p, fill);
    }
}
