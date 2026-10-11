package com.baiyou.pocket.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Canvas 逻辑坐标游戏基类。
 *
 *  - 游戏在固定逻辑坐标系 (0,0)-(lw,lh) 内绘制，宿主视图自动等比缩放并居中
 *    （手机/平板/PC 自动适配），触摸坐标自动反变换回逻辑坐标；
 *  - 提供受宿主兜底的安全逻辑帧循环：startLoop/setNextTick/stopLoop，
 *    onTick 内抛出的异常会上抛 host.crash()，不会静默卡死；
 *  - 内置共享特效系统：popup() 浮动文字、burst() 粒子爆裂、shake() 震屏，
 *    由独立的 16ms 特效循环驱动，渲染时自动叠加在 render() 之上；
 *  - 子类实现 render()/touch()/onTick()/restartGame()。
 *
 *  约定：游戏画面自成一体的配色（不随日夜主题切换），保证任何主题下观感一致。 */
public abstract class CanvasGame extends Game {
    protected final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    protected float lw, lh;          // 逻辑尺寸（子类构造时给定）
    protected View view;
    protected boolean over;          // 本局是否已结束
    protected final Random rnd = new Random();

    private float scale = 1f, offX, offY;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final RectF tmpRect = new RectF();
    private boolean looping;
    private boolean loopWasOn;         // 挂起时循环是否在跑（用于对称恢复）
    private long nextMs = 16;

    protected CanvasGame(float lw, float lh) {
        this.lw = lw;
        this.lh = lh;
        stroke.setStyle(Paint.Style.STROKE);
    }

    // ================= 视图与坐标 =================

    @Override
    public final View createView() {
        view = new View(ctx) {
            @Override
            protected void onSizeChanged(int w, int h, int ow, int oh) {
                fit(w, h);
            }

            @Override
            protected void onDraw(Canvas c) {
                c.save();
                // 震屏（设备像素级偏移，发生在缩放之前）
                long now = System.currentTimeMillis();
                if (now < shakeUntil) {
                    float k = (shakeUntil - now) / (float) shakeMs;
                    c.translate((rnd.nextFloat() - 0.5f) * 14 * k * scale,
                            (rnd.nextFloat() - 0.5f) * 14 * k * scale);
                }
                c.translate(offX, offY);
                c.scale(scale, scale);
                try {
                    render(c);
                    drawFx(c);
                } catch (Throwable t) {
                    host.crash(t);   // 绘制异常也交给宿主兜底
                } finally {
                    c.restore();
                }
            }
        };
        view.setOnTouchListener((v, ev) -> {
            final float lx = (ev.getX() - offX) / scale;
            final float ly = (ev.getY() - offY) / scale;
            final int act = ev.getActionMasked();
            runSafe(() -> touch(lx, ly, act));
            return true;
        });
        return view;
    }

    private void fit(int w, int h) {
        float s = Math.min(w / lw, h / lh);
        scale = s > 0 ? s : 1f;
        offX = (w - lw * scale) / 2f;
        offY = (h - lh * scale) / 2f;
    }

    protected final void dirty() {
        if (view != null) view.postInvalidateOnAnimation();
    }

    /** 逻辑坐标系尺寸（网页端按此 1:1 出帧，触点零换算）。 */
    public final float viewWidth() {
        return lw;
    }

    public final float viewHeight() {
        return lh;
    }

    // ================= 逻辑帧循环 =================

    /** 启动安全循环（游戏开始时调用）。 */
    protected final void startLoop(long firstDelayMs) {
        nextMs = firstDelayMs;
        if (looping) return;
        looping = true;
        handler.postDelayed(loop, Math.max(1, nextMs));
    }

    public final void stopLoop() {
        looping = false;
        handler.removeCallbacks(loop);
    }

    /** onTick 内设置下一帧间隔（默认 16ms），用于动态变速（如蛇加速）。 */
    protected final void setNextTick(long ms) {
        nextMs = ms;
    }

    /** 安全延时器：异常上抛宿主，进程被毁时随循环一起终止。 */
    protected final void postDelayedSafe(final Runnable r, long ms) {
        handler.postDelayed(() -> runSafe(r), ms);
    }

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!looping) return;
            nextMs = 16;
            runSafe(CanvasGame.this::onTick);
            if (!looping) return;
            handler.postDelayed(this, Math.max(1, nextMs));
        }
    };

    protected void onTick() {
    }

    // ================= 特效系统（浮动文字 / 粒子 / 震屏） =================

    /** {text, x, y, born, color, size} */
    private final List<Object[]> fxTexts = new ArrayList<>();
    /** {x, y, vx, vy, born, life, color, size} */
    private final List<float[]> fxParts = new ArrayList<>();
    private long shakeUntil, shakeMs;
    private boolean fxOn;

    private final Runnable fxLoop = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            Iterator<Object[]> ti = fxTexts.iterator();
            while (ti.hasNext()) if (now - (Long) ti.next()[3] > 760) ti.remove();
            Iterator<float[]> pi = fxParts.iterator();
            while (pi.hasNext()) {
                float[] p = pi.next();
                if (now - p[4] > p[5]) pi.remove();
                else {
                    p[0] += p[2];
                    p[1] += p[3];
                    p[2] *= 0.96f;
                    p[3] = p[3] * 0.96f + 0.05f; // 轻微重力
                }
            }
            boolean has = !fxTexts.isEmpty() || !fxParts.isEmpty() || now < shakeUntil;
            if (has) {
                dirty();
                handler.postDelayed(this, 16);
            } else {
                fxOn = false;
            }
        }
    };

    private void startFx() {
        if (fxOn) return;
        fxOn = true;
        handler.postDelayed(fxLoop, 16);
    }

    /** 浮动文字：从 (x,y) 上浮并淡出。 */
    protected final void popup(String text, float x, float y, int color) {
        popup(text, x, y, color, 20);
    }

    protected final void popup(String text, float x, float y, int color, float size) {
        fxTexts.add(new Object[]{text, x, y, System.currentTimeMillis(), color, size});
        startFx();
    }

    /** 粒子爆裂：n 个彩屑从 (x,y) 放射。 */
    protected final void burst(float x, float y, int color, int n, float speed) {
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float sp = speed * (0.4f + rnd.nextFloat() * 0.8f);
            fxParts.add(new float[]{x, y,
                    (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    System.currentTimeMillis(), 380 + rnd.nextInt(260),
                    color, 2 + rnd.nextFloat() * 3});
        }
        startFx();
    }

    /** 震屏（毫秒）。 */
    protected final void shake(int ms) {
        shakeMs = ms;
        shakeUntil = System.currentTimeMillis() + ms;
        startFx();
    }

    private void drawFx(Canvas c) {
        long now = System.currentTimeMillis();
        for (Object[] t : fxTexts) {
            float age = (now - (Long) t[3]) / 760f;
            int alpha = age < 0.6f ? 255 : (int) (255 * (1 - (age - 0.6f) / 0.4f));
            label(c, (String) t[0], (Float) t[1], (Float) t[2] - age * 46f,
                    (Float) t[5], (alpha << 24) | (0x00FFFFFF & (Integer) t[4]), true);
        }
        for (float[] p : fxParts) {
            float age = (now - p[4]) / p[5];
            int alpha = (int) (255 * (1 - age));
            fill.setColor((alpha << 24) | (0x00FFFFFF & (int) p[6]));
            c.drawCircle(p[0], p[1], p[7] * (1 - age * 0.5f), fill);
        }
    }

    // ================= 生命周期 =================

    @Override
    public void pauseGame() {
        loopWasOn = looping; // 记录挂起时是否有逻辑循环，恢复时对称重启
        stopLoop();
    }

    /** 默认恢复：挂起前有逻辑循环就按原节奏重启；需要自定义节奏的游戏仍可覆写。 */
    @Override
    public void resumeGame() {
        if (loopWasOn && !over) {
            loopWasOn = false;
            startLoop(Math.max(1, nextMs));
        }
    }

    @Override
    public void destroy() {
        stopLoop();
        handler.removeCallbacks(fxLoop);
    }

    // ================= 子类钩子 =================

    /** 逻辑坐标下渲染（特效由基类在其后自动叠加）。 */
    protected abstract void render(Canvas c);

    /** 逻辑坐标触摸。action：0=DOWN 1=UP 2=MOVE 3=CANCEL。 */
    protected void touch(float x, float y, int action) {
    }

    // ================= 绘制小工具 =================

    protected final void rr(Canvas c, float l, float t, float r, float b, float rad, int color) {
        tmpRect.set(l, t, r, b);
        fill.setColor(color);
        c.drawRoundRect(tmpRect, rad, rad, fill);
    }

    /** 居中文字（cx,cy 为视觉中心点）。 */
    protected final void label(Canvas c, String s, float cx, float cy, float size, int color, boolean bold) {
        txt.setColor(color);
        txt.setTextSize(size);
        txt.setFakeBoldText(bold);
        txt.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = txt.getFontMetrics();
        c.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, txt);
    }

    protected final void beep(int kind) {
        host.beep(kind);
    }

    /** 缓出插值（先快后慢），动画手感用。 */
    protected final float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1 - (1 - t) * (1 - t);
    }

    // ================= 通用小工具（家族基类与批量游戏共用） =================

    protected final long now() {
        return System.currentTimeMillis();
    }

    protected final float rand(float min, float max) {
        return min + rnd.nextFloat() * (max - min);
    }

    protected final int randi(int n) {
        return rnd.nextInt(n);
    }

    protected final boolean hit(float px, float py, float l, float t, float w, float h) {
        return px >= l && px <= l + w && py >= t && py <= t + h;
    }

    /** 文本宽度（按当前字体度量）。 */
    protected final float textW(String s, float size, boolean bold) {
        txt.setTextSize(size);
        txt.setFakeBoldText(bold);
        return txt.measureText(s);
    }

    /** 一行血量/命：实心圆=剩余，空心圆=已失。 */
    protected final void hearts(Canvas c, int lives, int max, float rightX, float cy, float r) {
        for (int i = 0; i < max; i++) {
            float x = rightX - i * (r * 2.6f);
            if (i < lives) {
                fill.setColor(0xFFE8574F);
                c.drawCircle(x, cy, r, fill);
            } else {
                fill.setColor(0x66808080);
                c.drawCircle(x, cy, r * 0.6f, fill);
            }
        }
    }

    /** 中文按字符硬换行（每行 maxChars 个字）。 */
    protected final String[] wrapCn(String s, int maxChars) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < s.length(); i += maxChars) {
            out.add(s.substring(i, Math.min(s.length(), i + maxChars)));
        }
        return out.toArray(new String[0]);
    }
}
