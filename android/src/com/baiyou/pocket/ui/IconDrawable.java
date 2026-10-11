package com.baiyou.pocket.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;

/** 游戏图标 v2：每款游戏一个专属程序化小 pictogram（100 款各不相同），
 *  底色也从"按分类一色"改为按游戏序号轮换的 12 色盘。
 *  三端共用：安卓首页、PC 首页、网页 /icon 均渲染本类。 */
public class IconDrawable extends Drawable {
    /** 每款游戏底色（按序号轮换 12 色盘）。 */
    private static final int[] BG = {
            0xFF5B8DEF, 0xFFE8574F, 0xFFF2A33C, 0xFF2FA36B, 0xFF9B59F6, 0xFF12B5A5,
            0xFFE85FA8, 0xFFF2784B, 0xFF4A90E2, 0xFF7C9A3D, 0xFF6C5CE7, 0xFFD46A9E,
    };
    private static final int LOCKED_BG = 0xFF9AA0AC;
    private static final int W = 0xFFFFFFFF;

    private final GameMeta meta;
    private final int idx;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();

    public IconDrawable(GameMeta meta) {
        this.meta = meta;
        this.idx = Registry.indexOf(meta.id);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    /** 该游戏的图标底色（网页首页卡片描边用）。 */
    public static int colorOf(GameMeta m) {
        int i = Registry.indexOf(m.id);
        return BG[((i % BG.length) + BG.length) % BG.length];
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        if (b.isEmpty()) return;
        float w = b.width(), h = b.height();
        float rad = Math.min(w, h) * 0.24f;
        rf.set(b);
        // 阴影直接画进图标里（离屏一次成型，避免前端 100 个 CSS 滤镜拖慢滚动）
        fill.setColor(0x55000000);
        canvas.drawRoundRect(rf, rad, rad, fill);
        float sh = Math.min(w, h) * 0.05f;
        rf.set(b.left, b.top - sh, b.right - sh, b.bottom - sh);
        fill.setColor(meta.playable() ? BG[((idx % BG.length) + BG.length) % BG.length] : LOCKED_BG);
        canvas.drawRoundRect(rf, rad, rad, fill);

        float cx = b.centerX() - sh / 2f;
        float cy = b.centerY() - sh / 2f;
        float u = Math.min(w, h) - sh;
        if (meta.playable()) {
            drawPictogram(canvas, idx, cx, cy, u);
        } else {
            // 未开放：挂锁
            float bodyW = u * 0.42f, bodyH = u * 0.3f;
            fill.setColor(W);
            rf.set(cx - bodyW / 2, cy - bodyH * 0.05f, cx + bodyW / 2, cy + bodyH * 0.55f);
            canvas.drawRoundRect(rf, bodyH * 0.2f, bodyH * 0.2f, fill);
            stroke.setColor(W);
            stroke.setStrokeWidth(u * 0.06f);
            rf.set(cx - bodyW * 0.3f, cy - bodyH * 0.75f, cx + bodyW * 0.3f, cy);
            canvas.drawArc(rf, 180, 180, false, stroke);
        }
    }

    // ================= 基元 =================

    private void disc(Canvas c, float x, float y, float r, int color) {
        fill.setColor(color);
        c.drawCircle(x, y, r, fill);
    }

    private void box(Canvas c, float l, float t, float r, float b, float rad, int color) {
        fill.setColor(color);
        rf.set(l, t, r, b);
        c.drawRoundRect(rf, rad, rad, fill);
    }

    private void bar(Canvas c, float x1, float y1, float x2, float y2, float w, int color) {
        stroke.setColor(color);
        stroke.setStrokeWidth(w);
        c.drawLine(x1, y1, x2, y2, stroke);
    }

    private void ringo(Canvas c, float x, float y, float r, float w, int color) {
        stroke.setColor(color);
        stroke.setStrokeWidth(w);
        c.drawCircle(x, y, r, stroke);
    }

    private void arc0(Canvas c, float x, float y, float r, float start, float sweep, float w, int color) {
        stroke.setColor(color);
        stroke.setStrokeWidth(w);
        rf.set(x - r, y - r, x + r, y + r);
        c.drawArc(rf, start, sweep, false, stroke);
    }

    private void tri(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        fill.setColor(color);
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.close();
        c.drawPath(p, fill);
    }

    private void txt(Canvas c, String s, float x, float y, float size, int color) {
        txt.setColor(color);
        txt.setTextSize(size);
        txt.setFakeBoldText(true);
        txt.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = txt.getFontMetrics();
        c.drawText(s, x, y - (fm.ascent + fm.descent) / 2f, txt);
    }

    private void card(Canvas c, float x, float y, float w, float h, int bg) {
        box(c, x, y, x + w, y + h, w * 0.14f, bg);
        stroke.setColor(0x331B1C22);
        stroke.setStrokeWidth(1);
        rf.set(x + w * 0.1f, y + h * 0.1f, x + w * 0.9f, y + h * 0.9f);
    }

    // ================= 100 款专属 pictogram =================
    // idx = g 序号 - 1。坐标以 (cx,cy) 为中心、u 为图标边长。

    private void drawPictogram(Canvas c, int idx, float cx, float cy, float u) {
        float h = u * 0.3f;   // 半宽基准
        switch (idx) {
            case 0: // 方块消消：四色小方块
                box(c, cx - h, cy - h, cx, cy, 2, W);
                box(c, cx + 1, cy - h, cx + h, cy, 2, 0xFFB9C4E0);
                box(c, cx - h, cy + 1, cx, cy + h, 2, 0xFFB9C4E0);
                box(c, cx + 1, cy + 1, cx + h, cy + h, 2, W);
                break;
            case 1: // 贪吃蛇
                bar(c, cx - h, cy + h * 0.5f, cx - h * 0.3f, cy - h * 0.6f, 5, W);
                bar(c, cx - h * 0.3f, cy - h * 0.6f, cx + h * 0.5f, cy - h * 0.6f, 5, W);
                bar(c, cx + h * 0.5f, cy - h * 0.6f, cx + h, cy + h * 0.3f, 5, W);
                disc(c, cx + h, cy + h * 0.3f, 4, W);
                disc(c, cx - h * 0.7f, cy + h * 0.3f, 3.5f, W);
                break;
            case 2: // 记忆翻牌
                card(c, cx - h, cy - h * 0.7f, h * 1.1f, h * 1.5f, 0x66FFFFFF);
                card(c, cx - h * 0.1f, cy - h * 0.55f, h * 1.1f, h * 1.5f, W);
                txt(c, "?", cx + h * 0.45f, cy + h * 0.2f, u * 0.26f, 0xFF5B8DEF);
                break;
            case 3: // 碎块弹球
                box(c, cx - h, cy - h * 0.8f, cx - h * 0.15f, cy - h * 0.55f, 1, W);
                box(c, cx, cy - h * 0.8f, cx + h, cy - h * 0.55f, 1, 0xFFB9C4E0);
                bar(c, cx - h * 0.6f, cy + h * 0.8f, cx + h * 0.6f, cy + h * 0.8f, 4, W);
                disc(c, cx, cy + h * 0.3f, 4, W);
                break;
            case 4: // 数字合并
                txt(c, "2", cx - h * 0.5f, cy - h * 0.5f, u * 0.22f, W);
                txt(c, "2", cx + h * 0.5f, cy - h * 0.5f, u * 0.22f, W);
                box(c, cx - h * 0.85f, cy - h * 0.9f, cx + h * 0.05f, cy - h * 0.1f, 2, 0x44FFFFFF);
                box(c, cx - h * 0.05f, cy - h * 0.9f, cx + h * 0.85f, cy - h * 0.1f, 2, 0x44FFFFFF);
                txt(c, "4", cx, cy + h * 0.55f, u * 0.26f, W);
                break;
            case 5: // 滑块拼图
                ringo(c, cx, cy, h * 0.85f, 3, W);
                bar(c, cx, cy - h * 0.85f, cx, cy + h * 0.85f, 2.5f, W);
                bar(c, cx - h * 0.85f, cy, cx + h * 0.85f, cy, 2.5f, W);
                break;
            case 6: // 数字华容道
                box(c, cx - h, cy - h, cx, cy, 2, W);
                box(c, cx, cy - h, cx + h, cy, 2, 0xFFB9C4E0);
                box(c, cx - h, cy, cx, cy + h, 2, 0xFFB9C4E0);
                box(c, cx, cy, cx + h, cy + h, 2, W);
                txt(c, "15", cx + h * 0.5f, cy + h * 0.5f, u * 0.16f, 0xFF5B8DEF);
                break;
            case 7: // 一笔画
                bar(c, cx - h, cy + h * 0.4f, cx - h * 0.3f, cy - h * 0.4f, 3.5f, W);
                bar(c, cx - h * 0.3f, cy - h * 0.4f, cx + h * 0.4f, cy + h * 0.1f, 3.5f, W);
                bar(c, cx + h * 0.4f, cy + h * 0.1f, cx + h, cy - h * 0.6f, 3.5f, W);
                disc(c, cx - h, cy + h * 0.4f, 3.5f, W);
                break;
            case 8: // 迷宫滚球
                box(c, cx - h * 0.8f, cy - h * 0.8f, cx + h * 0.8f, cy + h * 0.8f, 2, 0x44FFFFFF);
                box(c, cx - h * 0.45f, cy - h * 0.8f, cx - h * 0.15f, cy + h * 0.1f, 1, W);
                box(c, cx + h * 0.15f, cy - h * 0.1f, cx + h * 0.8f, cy + h * 0.15f, 1, W);
                disc(c, cx - h * 0.55f, cy + h * 0.5f, 4, W);
                break;
            case 9: // 点亮灯塔
                disc(c, cx, cy - h * 0.15f, h * 0.55f, 0x66FFFFFF);
                disc(c, cx, cy - h * 0.15f, h * 0.3f, W);
                bar(c, cx - h * 0.7f, cy + h * 0.7f, cx + h * 0.7f, cy + h * 0.7f, 4, W);
                break;
            case 10: // 孔明棋阵
                disc(c, cx - h * 0.5f, cy - h * 0.3f, h * 0.24f, W);
                disc(c, cx + h * 0.1f, cy - h * 0.55f, h * 0.24f, W);
                disc(c, cx + h * 0.5f, cy - h * 0.1f, h * 0.24f, W);
                ringo(c, cx - h * 0.2f, cy + h * 0.45f, h * 0.24f, 2.5f, W);
                break;
            case 11: // 管道拧转
                bar(c, cx - h, cy, cx, cy, 6, W);
                bar(c, cx, cy, cx, cy - h * 0.8f, 6, W);
                bar(c, cx, cy, cx + h * 0.7f, cy + h * 0.7f, 6, W);
                break;
            case 12: // 形状归位
                ringo(c, cx - h * 0.55f, cy - h * 0.3f, h * 0.3f, 2.5f, W);
                box(c, cx + h * 0.1f, cy - h * 0.62f, cx + h * 0.7f, cy - h * 0.02f, 2, W);
                tri(c, cx - h * 0.55f, cy + h * 0.15f, cx - h * 0.1f, cy + h * 0.85f, cx - h, cy + h * 0.85f, W);
                break;
            case 13: // 木箱推推
                box(c, cx - h * 0.75f, cy - h * 0.75f, cx + h * 0.05f, cy + h * 0.05f, 3, W);
                bar(c, cx - h * 0.75f, cy - h * 0.75f, cx + h * 0.05f, cy + h * 0.05f, 2.5f, 0xFF5B8DEF);
                box(c, cx - h * 0.05f, cy - h * 0.05f, cx + h * 0.75f, cy + h * 0.75f, 3, 0xFFB9C4E0);
                break;
            case 14: // 算式速配
                txt(c, "+", cx - h * 0.45f, cy, u * 0.3f, W);
                bar(c, cx - h * 0.05f, cy + h * 0.35f, cx + h * 0.4f, cy + h * 0.35f, 3, W);
                txt(c, "=", cx + h * 0.6f, cy, u * 0.26f, W);
                break;
            case 15: // 心算冲刺
                txt(c, "×", cx, cy - h * 0.15f, u * 0.34f, W);
                bar(c, cx - h * 0.6f, cy + h * 0.6f, cx + h * 0.6f, cy + h * 0.6f, 3, W);
                break;
            case 16: // 数独小栈
                ringo(c, cx, cy, h * 0.85f, 2.5f, W);
                bar(c, cx, cy - h * 0.85f, cx, cy + h * 0.85f, 2, W);
                bar(c, cx - h * 0.85f, cy, cx + h * 0.85f, cy, 2, W);
                disc(c, cx - h * 0.42f, cy - h * 0.42f, 3, W);
                disc(c, cx + h * 0.42f, cy + h * 0.42f, 3, W);
                break;
            case 17: // 数字链消
                ringo(c, cx - h * 0.45f, cy, h * 0.45f, 3.5f, W);
                ringo(c, cx + h * 0.45f, cy, h * 0.45f, 3.5f, W);
                txt(c, "10", cx, cy, u * 0.18f, W);
                break;
            case 18: // 廿四快斗
                txt(c, "24", cx, cy, u * 0.34f, W);
                break;
            case 19: // 汉诺塔影
                box(c, cx - h * 0.8f, cy + h * 0.5f, cx + h * 0.8f, cy + h * 0.75f, 2, W);
                box(c, cx - h * 0.6f, cy + h * 0.15f, cx + h * 0.6f, cy + h * 0.4f, 2, 0xFFB9C4E0);
                box(c, cx - h * 0.4f, cy - h * 0.2f, cx + h * 0.4f, cy + h * 0.05f, 2, 0xFFB9C4E0);
                box(c, cx - h * 0.2f, cy - h * 0.55f, cx + h * 0.2f, cy - h * 0.3f, 2, W);
                break;
            case 20: // 素数猎手
                ringo(c, cx, cy, h * 0.75f, 3, W);
                txt(c, "7", cx, cy, u * 0.3f, W);
                break;
            case 21: // 天平砝码
                bar(c, cx - h * 0.8f, cy - h * 0.3f, cx + h * 0.8f, cy - h * 0.1f, 3.5f, W);
                bar(c, cx, cy - h * 0.2f, cx, cy + h * 0.55f, 3.5f, W);
                box(c, cx - h * 0.55f, cy + h * 0.5f, cx + h * 0.55f, cy + h * 0.75f, 2, W);
                break;
            case 22: // 递增序列
                bar(c, cx - h * 0.7f, cy + h * 0.7f, cx - h * 0.7f, cy + h * 0.2f, 5, W);
                bar(c, cx, cy + h * 0.7f, cx, cy - h * 0.2f, 5, W);
                bar(c, cx + h * 0.7f, cy + h * 0.7f, cx + h * 0.7f, cy - h * 0.7f, 5, W);
                break;
            case 23: // 手速连点
                tri(c, cx + h * 0.1f, cy - h * 0.9f, cx - h * 0.55f, cy + h * 0.15f, cx - h * 0.05f, cy + h * 0.15f, W);
                tri(c, cx - h * 0.1f, cy + h * 0.9f, cx + h * 0.55f, cy - h * 0.15f, cx + h * 0.05f, cy - h * 0.15f, W);
                break;
            case 24: // 色觉快答
                disc(c, cx - h * 0.35f, cy - h * 0.2f, h * 0.5f, 0x88FFFFFF);
                disc(c, cx + h * 0.35f, cy - h * 0.2f, h * 0.5f, 0x88FFFFFF);
                txt(c, "橙", cx, cy + h * 0.55f, u * 0.2f, W);
                break;
            case 25: // 惊现捕手
                ringo(c, cx, cy, h * 0.7f, 3, W);
                bar(c, cx - h * 0.95f, cy, cx - h * 0.45f, cy, 3, W);
                bar(c, cx + h * 0.45f, cy, cx + h * 0.95f, cy, 3, W);
                bar(c, cx, cy - h * 0.95f, cx, cy - h * 0.45f, 3, W);
                bar(c, cx, cy + h * 0.45f, cx, cy + h * 0.95f, 3, W);
                disc(c, cx, cy, 4, W);
                break;
            case 26: // 黑块快弹
                box(c, cx - h, cy - h, cx, cy + h, 2, 0x55FFFFFF);
                box(c, cx, cy - h, cx + h, cy + h, 2, 0x55FFFFFF);
                box(c, cx - h, cy - h * 0.25f, cx, cy + h * 0.75f, 2, W);
                break;
            case 27: // 地鼠快打
                arc0(c, cx, cy + h * 0.5f, h * 0.75f, 180, 180, 3, W);
                disc(c, cx - h * 0.25f, cy - h * 0.15f, 2.5f, W);
                disc(c, cx + h * 0.25f, cy - h * 0.15f, 2.5f, W);
                disc(c, cx, cy + h * 0.2f, 2, W);
                break;
            case 28: // 快刀削削
                disc(c, cx - h * 0.3f, cy - h * 0.35f, h * 0.42f, W);
                bar(c, cx - h * 0.7f, cy - h * 0.35f, cx + h * 0.7f, cy + h * 0.5f, 4, W);
                break;
            case 29: // 红区闪避
                tri(c, cx, cy - h * 0.8f, cx + h * 0.85f, cy + h * 0.6f, cx - h * 0.85f, cy + h * 0.6f, W);
                txt(c, "!", cx, cy + h * 0.22f, u * 0.3f, 0xFF5B8DEF);
                break;
            case 30: // 刻度停针
                ringo(c, cx, cy, h * 0.8f, 3, W);
                bar(c, cx, cy, cx, cy - h * 0.7f, 3.5f, W);
                disc(c, cx, cy, 3.5f, W);
                break;
            case 31: // 节奏连击
                txt(c, "♪", cx, cy, u * 0.42f, W);
                break;
            case 32: // 眼疾手快
                disc(c, cx, cy, h * 0.5f, W);
                disc(c, cx, cy, h * 0.26f, 0xFF5B8DEF);
                break;
            case 33: // 星屑闪避
                tri(c, cx, cy - h * 0.85f, cx - h * 0.55f, cy + h * 0.7f, cx + h * 0.55f, cy + h * 0.7f, W);
                disc(c, cx, cy - h * 0.15f, h * 0.16f, 0xFF5B8DEF);
                break;
            case 34: // 弹跳小虾
                arc0(c, cx, cy + h * 0.55f, h * 0.6f, 180, 180, 3, W);
                disc(c, cx, cy - h * 0.45f, h * 0.32f, W);
                bar(c, cx, cy - h * 0.13f, cx, cy + h * 0.23f, 3, W);
                break;
            case 35: // 星尘阻击
                box(c, cx - h * 0.75f, cy - h * 0.35f, cx - h * 0.35f, cy + h * 0.05f, 1, W);
                box(c, cx - h * 0.2f, cy - h * 0.65f, cx + h * 0.2f, cy - h * 0.25f, 1, W);
                box(c, cx + h * 0.35f, cy - h * 0.35f, cx + h * 0.75f, cy + h * 0.05f, 1, W);
                box(c, cx - h * 0.5f, cy + h * 0.15f, cx - h * 0.1f, cy + h * 0.55f, 1, W);
                box(c, cx + h * 0.1f, cy + h * 0.15f, cx + h * 0.5f, cy + h * 0.55f, 1, W);
                break;
            case 36: // 蹦跳忍者
                disc(c, cx, cy - h * 0.35f, h * 0.42f, W);
                box(c, cx - h * 0.4f, cy + h * 0.1f, cx + h * 0.4f, cy + h * 0.75f, 2, W);
                bar(c, cx - h * 0.42f, cy - h * 0.42f, cx + h * 0.42f, cy - h * 0.42f, 3, 0xFFB9C4E0);
                break;
            case 37: // 无尽坠落
                bar(c, cx, cy - h * 0.8f, cx, cy + h * 0.6f, 3.5f, W);
                tri(c, cx, cy + h * 0.85f, cx - h * 0.35f, cy + h * 0.4f, cx + h * 0.35f, cy + h * 0.4f, W);
                break;
            case 38: // 圆环穿梭
                ringo(c, cx, cy, h * 0.85f, 3, W);
                ringo(c, cx, cy, h * 0.45f, 3, W);
                disc(c, cx, cy, 4, W);
                break;
            case 39: // 果冻跳跳
                fill.setColor(W);
                rf.set(cx - h * 0.6f, cy - h * 0.35f, cx + h * 0.6f, cy + h * 0.65f);
                c.drawRoundRect(rf, h * 0.35f, h * 0.35f, fill);
                disc(c, cx - h * 0.2f, cy - h * 0.05f, 2.5f, 0xFF5B8DEF);
                disc(c, cx + h * 0.2f, cy - h * 0.05f, 2.5f, 0xFF5B8DEF);
                break;
            case 40: // 藤蔓飞跃
                arc0(c, cx + h * 0.3f, cy - h * 0.7f, h * 0.8f, 110, 130, 3.5f, W);
                disc(c, cx, cy + h * 0.45f, h * 0.28f, W);
                break;
            case 41: // 序列复现
                disc(c, cx - h * 0.45f, cy - h * 0.45f, h * 0.3f, W);
                ringo(c, cx + h * 0.45f, cy - h * 0.45f, h * 0.3f, 2.5f, W);
                ringo(c, cx - h * 0.45f, cy + h * 0.45f, h * 0.3f, 2.5f, W);
                disc(c, cx + h * 0.45f, cy + h * 0.45f, h * 0.3f, W);
                break;
            case 42: // 位置回想
                box(c, cx - h * 0.8f, cy - h * 0.8f, cx + h * 0.8f, cy + h * 0.8f, 2, 0x44FFFFFF);
                disc(c, cx - h * 0.4f, cy - h * 0.4f, h * 0.17f, W);
                disc(c, cx + h * 0.1f, cy, h * 0.17f, W);
                disc(c, cx - h * 0.15f, cy + h * 0.45f, h * 0.17f, W);
                break;
            case 43: // 图案速记
                card(c, cx - h * 0.65f, cy - h * 0.85f, h * 1.3f, h * 1.7f, W);
                txt(c, "?", cx, cy, u * 0.3f, 0xFF5B8DEF);
                break;
            case 44: // 瞬时记忆
                txt(c, "123", cx, cy, u * 0.26f, W);
                break;
            case 45: // 符号对对碰
                card(c, cx - h * 0.85f, cy - h * 0.6f, h * 0.9f, h * 1.3f, 0x66FFFFFF);
                card(c, cx - h * 0.05f, cy - h * 0.6f, h * 0.9f, h * 1.3f, W);
                txt(c, "A", cx + h * 0.4f, cy + h * 0.05f, u * 0.18f, 0xFF5B8DEF);
                break;
            case 46: // 卡片棋局
                card(c, cx - h * 0.7f, cy - h * 0.75f, h * 1.05f, h * 1.5f, W);
                txt(c, "K", cx - h * 0.18f, cy, u * 0.22f, 0xFFE8574F);
                ringo(c, cx + h * 0.5f, cy + h * 0.55f, h * 0.24f, 2.5f, W);
                break;
            case 47: // 色彩记忆
                disc(c, cx - h * 0.62f, cy, h * 0.19f, W);
                disc(c, cx - h * 0.21f, cy, h * 0.19f, 0xFFB9C4E0);
                disc(c, cx + h * 0.2f, cy, h * 0.19f, W);
                disc(c, cx + h * 0.61f, cy, h * 0.19f, 0xFFB9C4E0);
                break;
            case 48: // 数字追踪
                disc(c, cx - h * 0.55f, cy + h * 0.5f, 3.5f, W);
                disc(c, cx, cy, 3.5f, W);
                disc(c, cx + h * 0.55f, cy - h * 0.5f, 3.5f, W);
                bar(c, cx - h * 0.55f, cy + h * 0.5f, cx, cy, 2, W);
                bar(c, cx, cy, cx + h * 0.55f, cy - h * 0.5f, 2, W);
                txt(c, "1", cx - h * 0.55f, cy + h * 0.2f, u * 0.14f, W);
                txt(c, "3", cx + h * 0.55f, cy - h * 0.8f, u * 0.14f, W);
                break;
            case 49: // 细节侦探
                ringo(c, cx - h * 0.15f, cy - h * 0.15f, h * 0.5f, 3, W);
                bar(c, cx + h * 0.25f, cy + h * 0.25f, cx + h * 0.75f, cy + h * 0.75f, 4, W);
                break;
            case 50: // 经典接龙
                card(c, cx - h * 0.8f, cy - h * 0.55f, h * 0.85f, h * 1.25f, 0x66FFFFFF);
                card(c, cx - h * 0.25f, cy - h * 0.4f, h * 0.85f, h * 1.25f, W);
                card(c, cx + h * 0.3f, cy - h * 0.25f, h * 0.85f, h * 1.25f, 0xFFB9C4E0);
                break;
            case 51: // 空当接龙
                box(c, cx - h * 0.8f, cy - h * 0.75f, cx - h * 0.15f, cy - h * 0.15f, 2, 0x55FFFFFF);
                box(c, cx + h * 0.15f, cy - h * 0.75f, cx + h * 0.8f, cy - h * 0.15f, 2, 0x55FFFFFF);
                card(c, cx - h * 0.5f, cy + h * 0.05f, h, h * 1.3f, W);
                break;
            case 52: // 数字牌接龙
                card(c, cx - h * 0.55f, cy - h * 0.7f, h * 1.1f, h * 1.5f, W);
                txt(c, "5", cx, cy, u * 0.24f, 0xFFE8574F);
                bar(c, cx + h * 0.65f, cy - h * 0.4f, cx + h * 0.65f, cy + h * 0.55f, 2.5f, W);
                break;
            case 53: // 三堆翻牌
                box(c, cx - h * 0.8f, cy - h * 0.3f, cx - h * 0.25f, cy + h * 0.7f, 2, W);
                box(c, cx - h * 0.28f, cy - h * 0.45f, cx + h * 0.28f, cy + h * 0.55f, 2, 0xFFB9C4E0);
                box(c, cx + h * 0.25f, cy - h * 0.6f, cx + h * 0.8f, cy + h * 0.4f, 2, W);
                break;
            case 54: // 算牌速配
                txt(c, "A", cx - h * 0.5f, cy, u * 0.26f, W);
                txt(c, "+", cx, cy, u * 0.24f, W);
                txt(c, "K", cx + h * 0.5f, cy, u * 0.26f, W);
                break;
            case 55: // 大小接龙
                txt(c, "3", cx - h * 0.55f, cy, u * 0.28f, W);
                bar(c, cx - h * 0.2f, cy, cx + h * 0.25f, cy, 3, W);
                tri(c, cx + h * 0.25f, cy - h * 0.3f, cx + h * 0.25f, cy + h * 0.3f, cx + h * 0.65f, cy, W);
                txt(c, "4", cx + h * 0.6f, cy, u * 0.28f, 0xFFB9C4E0);
                break;
            case 56: // 花色分堆
                txt(c, "♠", cx - h * 0.5f, cy - h * 0.4f, u * 0.22f, W);
                txt(c, "♥", cx + h * 0.5f, cy - h * 0.4f, u * 0.22f, W);
                txt(c, "♦", cx - h * 0.5f, cy + h * 0.5f, u * 0.22f, W);
                txt(c, "♣", cx + h * 0.5f, cy + h * 0.5f, u * 0.22f, W);
                break;
            case 57: // 牌塔叠叠
                card(c, cx - h * 0.75f, cy + h * 0.15f, h * 0.7f, h * 0.95f, 0x66FFFFFF);
                card(c, cx + h * 0.05f, cy + h * 0.15f, h * 0.7f, h * 0.95f, 0xFFB9C4E0);
                card(c, cx - h * 0.35f, cy - h * 0.7f, h * 0.7f, h * 0.95f, W);
                break;
            case 58: // 残局闯关
                bar(c, cx, cy - h * 0.8f, cx, cy + h * 0.7f, 3.5f, W);
                tri(c, cx, cy - h * 0.8f, cx - h * 0.6f, cy - h * 0.45f, cx, cy - h * 0.1f, W);
                box(c, cx - h * 0.55f, cy + h * 0.6f, cx + h * 0.55f, cy + h * 0.85f, 2, W);
                break;
            case 59: // 记忆牌战
                card(c, cx - h * 0.7f, cy - h * 0.6f, h * 1.0f, h * 1.35f, 0x66FFFFFF);
                card(c, cx - h * 0.1f, cy - h * 0.6f, h * 1.0f, h * 1.35f, W);
                txt(c, "VS", cx + h * 0.4f, cy + h * 0.1f, u * 0.16f, 0xFF5B8DEF);
                break;
            case 60: // 泡泡射击
                disc(c, cx - h * 0.45f, cy - h * 0.4f, h * 0.3f, W);
                disc(c, cx + h * 0.25f, cy - h * 0.45f, h * 0.3f, 0xFFB9C4E0);
                disc(c, cx + h * 0.5f, cy + h * 0.15f, h * 0.3f, W);
                disc(c, cx - h * 0.1f, cy + h * 0.45f, h * 0.3f, 0xFFB9C4E0);
                break;
            case 61: // 切绳送糖
                bar(c, cx, cy - h * 0.9f, cx + h * 0.35f, cy - h * 0.1f, 2.5f, W);
                disc(c, cx + h * 0.35f, cy + h * 0.1f, h * 0.32f, W);
                break;
            case 62: // 涂色小格
                bar(c, cx - h * 0.6f, cy + h * 0.6f, cx + h * 0.55f, cy - h * 0.55f, 5, W);
                tri(c, cx + h * 0.45f, cy - h * 0.45f, cx + h * 0.8f, cy - h * 0.8f, cx + h * 0.85f, cy - h * 0.4f, W);
                break;
            case 63: // 弹珠台
                disc(c, cx - h * 0.45f, cy - h * 0.35f, 3, W);
                disc(c, cx + h * 0.05f, cy - h * 0.15f, 3, W);
                disc(c, cx - h * 0.25f, cy + h * 0.15f, 3, W);
                disc(c, cx + h * 0.35f, cy + h * 0.4f, 3, W);
                bar(c, cx - h * 0.7f, cy - h * 0.7f, cx - h * 0.7f, cy + h * 0.8f, 2.5f, W);
                bar(c, cx + h * 0.7f, cy - h * 0.7f, cx + h * 0.7f, cy + h * 0.8f, 2.5f, W);
                break;
            case 64: // 吊塔堆塔
                box(c, cx - h * 0.55f, cy + h * 0.45f, cx + h * 0.55f, cy + h * 0.8f, 2, 0xFFB9C4E0);
                box(c, cx - h * 0.4f, cy + h * 0.05f, cx + h * 0.4f, cy + h * 0.4f, 2, W);
                box(c, cx + h * 0.05f, cy - h * 0.5f, cx + h * 0.75f, cy - h * 0.15f, 2, 0xFFB9C4E0);
                break;
            case 65: // 抽绳解谜
                bar(c, cx - h * 0.8f, cy - h * 0.6f, cx + h * 0.3f, cy + h * 0.1f, 3, W);
                bar(c, cx + h * 0.3f, cy + h * 0.1f, cx - h * 0.3f, cy - h * 0.05f, 3, W);
                bar(c, cx - h * 0.3f, cy - h * 0.05f, cx + h * 0.8f, cy + h * 0.65f, 3, W);
                disc(c, cx + h * 0.3f, cy + h * 0.1f, 3, W);
                break;
            case 66: // 挖矿寻宝
                bar(c, cx - h * 0.5f, cy - h * 0.5f, cx + h * 0.45f, cy + h * 0.45f, 4.5f, W);
                bar(c, cx + h * 0.1f, cy - h * 0.75f, cx - h * 0.15f, cy + h * 0.2f, 4.5f, W);
                break;
            case 67: // 转盘迷宫
                arc0(c, cx, cy, h * 0.8f, 20, 300, 3.5f, W);
                arc0(c, cx, cy, h * 0.42f, 200, 300, 3.5f, W);
                disc(c, cx, cy, 4, W);
                break;
            case 68: // 纸飞机
                tri(c, cx - h * 0.8f, cy + h * 0.35f, cx + h * 0.85f, cy - h * 0.5f, cx - h * 0.1f, cy + h * 0.7f, W);
                bar(c, cx + h * 0.85f, cy - h * 0.5f, cx - h * 0.1f, cy + h * 0.7f, 2, 0xFFB9C4E0);
                break;
            case 69: // 挪车出库
                box(c, cx - h * 0.7f, cy - h * 0.45f, cx + h * 0.7f, cy + h * 0.25f, 3, W);
                disc(c, cx - h * 0.4f, cy + h * 0.45f, h * 0.18f, W);
                disc(c, cx + h * 0.4f, cy + h * 0.45f, h * 0.18f, W);
                break;
            case 70: // 点球大战
                box(c, cx - h * 0.85f, cy - h * 0.6f, cx + h * 0.85f, cy + h * 0.6f, 2, 0x55FFFFFF);
                disc(c, cx - h * 0.1f, cy + h * 0.15f, h * 0.22f, W);
                break;
            case 71: // 投篮大师
                ringo(c, cx, cy - h * 0.35f, h * 0.42f, 3.5f, W);
                bar(c, cx - h * 0.42f, cy - h * 0.35f, cx + h * 0.42f, cy - h * 0.35f, 2.5f, W);
                bar(c, cx, cy - h * 0.35f, cx, cy + h * 0.75f, 3, W);
                break;
            case 72: // 迷你高尔夫
                bar(c, cx + h * 0.45f, cy - h * 0.8f, cx + h * 0.45f, cy + h * 0.4f, 3, W);
                tri(c, cx + h * 0.45f, cy - h * 0.8f, cx - h * 0.1f, cy - h * 0.6f, cx + h * 0.45f, cy - h * 0.4f, W);
                disc(c, cx - h * 0.3f, cy + h * 0.55f, 4.5f, W);
                break;
            case 73: // 桌球新手
                disc(c, cx - h * 0.4f, cy - h * 0.3f, h * 0.24f, W);
                disc(c, cx + h * 0.25f, cy - h * 0.05f, h * 0.24f, 0xFFB9C4E0);
                disc(c, cx - h * 0.1f, cy + h * 0.45f, h * 0.24f, W);
                break;
            case 74: // 乒乓速对
                box(c, cx - h * 0.15f, cy - h * 0.55f, cx + h * 0.15f, cy + h * 0.45f, 2, W);
                disc(c, cx + h * 0.5f, cy - h * 0.3f, h * 0.2f, W);
                break;
            case 75: // 羽毛球挑高
                disc(c, cx, cy - h * 0.25f, h * 0.22f, W);
                tri(c, cx, cy - h * 0.1f, cx - h * 0.3f, cy + h * 0.65f, cx + h * 0.3f, cy + h * 0.65f, W);
                break;
            case 76: // 滑雪竞速
                bar(c, cx - h * 0.7f, cy + h * 0.6f, cx + h * 0.7f, cy + h * 0.6f, 3.5f, W);
                disc(c, cx - h * 0.15f, cy - h * 0.15f, h * 0.28f, W);
                bar(c, cx + h * 0.15f, cy - h * 0.5f, cx + h * 0.45f, cy - h * 0.85f, 2.5f, W);
                break;
            case 77: // 跨栏小将
                bar(c, cx - h * 0.75f, cy - h * 0.3f, cx + h * 0.75f, cy - h * 0.3f, 3.5f, W);
                bar(c, cx - h * 0.6f, cy - h * 0.3f, cx - h * 0.6f, cy + h * 0.7f, 3, W);
                bar(c, cx + h * 0.6f, cy - h * 0.3f, cx + h * 0.6f, cy + h * 0.7f, 3, W);
                break;
            case 78: // 射箭小屋
                arc0(c, cx - h * 0.3f, cy, h * 0.75f, -60, 120, 3.5f, W);
                bar(c, cx - h * 0.3f, cy - h * 0.7f, cx - h * 0.3f, cy + h * 0.7f, 2, W);
                bar(c, cx - h * 0.3f, cy, cx + h * 0.75f, cy, 2.5f, W);
                break;
            case 79: // 保龄球道
                tri(c, cx - h * 0.15f, cy - h * 0.85f, cx - h * 0.45f, cy + h * 0.1f, cx + h * 0.15f, cy + h * 0.1f, W);
                disc(c, cx + h * 0.35f, cy + h * 0.35f, h * 0.3f, W);
                break;
            case 80: // 星际防线
                Path sh = new Path();
                sh.moveTo(cx, cy - h * 0.85f);
                sh.lineTo(cx - h * 0.7f, cy - h * 0.4f);
                sh.lineTo(cx - h * 0.55f, cy + h * 0.15f);
                sh.lineTo(cx + h * 0.55f, cy + h * 0.15f);
                sh.lineTo(cx + h * 0.7f, cy - h * 0.4f);
                sh.close();
                fill.setColor(W);
                c.drawPath(sh, fill);
                bar(c, cx - h * 0.4f, cy - h * 0.05f, cx + h * 0.4f, cy - h * 0.05f, 3, 0xFF5B8DEF);
                break;
            case 81: // 弹弓打靶
                bar(c, cx - h * 0.55f, cy - h * 0.55f, cx, cy, 3.5f, W);
                bar(c, cx + h * 0.55f, cy - h * 0.55f, cx, cy, 3.5f, W);
                disc(c, cx - h * 0.55f, cy - h * 0.55f, 3.5f, W);
                disc(c, cx + h * 0.55f, cy - h * 0.55f, 3.5f, W);
                bar(c, cx - h * 0.55f, cy - h * 0.55f, cx - h * 0.55f, cy + h * 0.75f, 3, W);
                bar(c, cx + h * 0.55f, cy - h * 0.55f, cx + h * 0.55f, cy + h * 0.75f, 3, W);
                break;
            case 82: // 疾速神枪
                ringo(c, cx, cy, h * 0.75f, 3, W);
                disc(c, cx, cy, h * 0.24f, W);
                bar(c, cx - h * 0.95f, cy, cx + h * 0.95f, cy, 2, W);
                bar(c, cx, cy - h * 0.95f, cx, cy + h * 0.95f, 2, W);
                break;
            case 83: // 火箭拦截
                tri(c, cx, cy - h * 0.85f, cx - h * 0.38f, cy + h * 0.25f, cx + h * 0.38f, cy + h * 0.25f, W);
                bar(c, cx - h * 0.38f, cy + h * 0.3f, cx, cy + h * 0.8f, 2.5f, W);
                bar(c, cx + h * 0.38f, cy + h * 0.3f, cx, cy + h * 0.8f, 2.5f, W);
                break;
            case 84: // 炮台角度
                box(c, cx - h * 0.7f, cy + h * 0.35f, cx + h * 0.7f, cy + h * 0.8f, 2, W);
                bar(c, cx, cy + h * 0.35f, cx + h * 0.6f, cy - h * 0.45f, 5, W);
                break;
            case 85: // 深海鱼雷
                fill.setColor(W);
                rf.set(cx - h * 0.8f, cy - h * 0.28f, cx + h * 0.55f, cy + h * 0.28f);
                c.drawRoundRect(rf, h * 0.28f, h * 0.28f, fill);
                tri(c, cx + h * 0.55f, cy - h * 0.28f, cx + h * 0.9f, cy, cx + h * 0.55f, cy + h * 0.28f, W);
                break;
            case 86: // 弓箭守城
                box(c, cx - h * 0.7f, cy - h * 0.1f, cx + h * 0.7f, cy + h * 0.8f, 2, W);
                box(c, cx - h * 0.45f, cy - h * 0.55f, cx + h * 0.45f, cy - h * 0.1f, 2, 0xFFB9C4E0);
                break;
            case 87: // 太空弹幕
                disc(c, cx, cy - h * 0.35f, 3.5f, W);
                disc(c, cx - h * 0.45f, cy + h * 0.05f, 3.5f, 0xFFB9C4E0);
                disc(c, cx + h * 0.45f, cy + h * 0.05f, 3.5f, W);
                disc(c, cx, cy + h * 0.5f, 3.5f, 0xFFB9C4E0);
                break;
            case 88: // 激光折角
                box(c, cx - h * 0.25f, cy - h * 0.25f, cx + h * 0.25f, cy + h * 0.25f, 1, W);
                bar(c, cx - h, cy + h * 0.5f, cx - h * 0.1f, cy + h * 0.5f, 3, W);
                bar(c, cx - h * 0.1f, cy + h * 0.5f, cx + h * 0.1f, cy - h * 0.1f, 3, W);
                bar(c, cx + h * 0.1f, cy - h * 0.1f, cx + h, cy - h * 0.1f, 3, W);
                break;
            case 89: // 磁暴连射
                tri(c, cx - h * 0.35f, cy - h * 0.9f, cx - h * 0.75f, cy + h * 0.1f, cx - h * 0.05f, cy + h * 0.1f, W);
                tri(c, cx + h * 0.35f, cy - h * 0.3f, cx - h * 0.15f, cy + h * 0.9f, cx + h * 0.55f, cy + h * 0.35f, W);
                break;
            case 90: // 成语填空
                bar(c, cx - h * 0.75f, cy - h * 0.35f, cx + h * 0.75f, cy - h * 0.35f, 3, W);
                bar(c, cx - h * 0.75f, cy + h * 0.1f, cx + h * 0.2f, cy + h * 0.1f, 3, W);
                bar(c, cx - h * 0.75f, cy + h * 0.55f, cx + h * 0.75f, cy + h * 0.55f, 3, W);
                break;
            case 91: // 字谜猜猜
                txt(c, "?", cx, cy, u * 0.4f, W);
                break;
            case 92: // 诗词接句
                disc(c, cx + h * 0.45f, cy - h * 0.45f, h * 0.3f, 0x66FFFFFF);
                bar(c, cx - h * 0.8f, cy + h * 0.1f, cx + h * 0.2f, cy + h * 0.1f, 3, W);
                bar(c, cx - h * 0.8f, cy + h * 0.55f, cx + h * 0.45f, cy + h * 0.55f, 3, W);
                break;
            case 93: // 字母词链
                ringo(c, cx - h * 0.45f, cy, h * 0.42f, 3.5f, W);
                ringo(c, cx + h * 0.45f, cy, h * 0.42f, 3.5f, W);
                bar(c, cx - h * 0.12f, cy, cx + h * 0.12f, cy, 3, W);
                break;
            case 94: // 汉字找茬
                disc(c, cx - h * 0.42f, cy, h * 0.4f, 0x66FFFFFF);
                disc(c, cx + h * 0.42f, cy, h * 0.4f, W);
                break;
            case 95: // 拼字大师
                box(c, cx - h * 0.8f, cy - h * 0.4f, cx - h * 0.1f, cy + h * 0.3f, 2, W);
                box(c, cx + h * 0.1f, cy - h * 0.4f, cx + h * 0.8f, cy + h * 0.3f, 2, 0xFFB9C4E0);
                bar(c, cx - h * 0.8f, cy + h * 0.55f, cx + h * 0.8f, cy + h * 0.55f, 2.5f, W);
                break;
            case 96: // 词语排序
                bar(c, cx - h * 0.8f, cy - h * 0.4f, cx + h * 0.2f, cy - h * 0.4f, 3.5f, W);
                bar(c, cx - h * 0.8f, cy + h * 0.05f, cx + h * 0.5f, cy + h * 0.05f, 3.5f, 0xFFB9C4E0);
                bar(c, cx - h * 0.8f, cy + h * 0.5f, cx + h * 0.75f, cy + h * 0.5f, 3.5f, W);
                break;
            case 97: // 谜语小铺
                arc0(c, cx, cy - h * 0.15f, h * 0.55f, 180, 180, 3.5f, W);
                tri(c, cx - h * 0.55f, cy - h * 0.15f, cx + h * 0.55f, cy - h * 0.15f, cx, cy + h * 0.75f, W);
                break;
            case 98: // 部首组字
                box(c, cx - h * 0.8f, cy - h * 0.5f, cx - h * 0.15f, cy + h * 0.15f, 2, W);
                txt(c, "+", cx, cy - h * 0.18f, u * 0.18f, W);
                box(c, cx + h * 0.15f, cy - h * 0.5f, cx + h * 0.8f, cy + h * 0.15f, 2, W);
                bar(c, cx - h * 0.3f, cy + h * 0.55f, cx + h * 0.3f, cy + h * 0.55f, 3, W);
                break;
            default: // g100 单词消消
                box(c, cx - h * 0.8f, cy - h * 0.75f, cx + h * 0.8f, cy + h * 0.75f, 2, 0x44FFFFFF);
                bar(c, cx - h * 0.55f, cy - h * 0.3f, cx - h * 0.55f, cy + h * 0.35f, 3, W);
                bar(c, cx, cy - h * 0.3f, cx, cy + h * 0.35f, 3, 0xFFB9C4E0);
                bar(c, cx + h * 0.55f, cy - h * 0.3f, cx + h * 0.55f, cy + h * 0.35f, 3, W);
                break;
        }
    }

    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
