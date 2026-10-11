package com.baiyou.pocket.game;

import android.graphics.Canvas;

/** 序列记忆家族基类（Simon 类）：若干感应板按随机序列闪亮，
 *  玩家按序复现；序列每轮 +1，出错即结算；等级即得分（level×10）。 */
public abstract class SeqMemoryGame extends CanvasGame {
    protected static final int SHOW = 0, INPUT = 1;

    protected int[] pads;          // 本轮序列（板下标）
    protected int seqLen, inputPos;
    protected int phase = SHOW;
    private int showIdx;
    private long showUntil;
    private int litPad = -1;
    protected int level;

    protected SeqMemoryGame(float w, float h) {
        super(w, h);
    }

    /** 板数（子类定）。 */
    protected abstract int padCount();

    /** 板 i 的中心坐标。 */
    protected abstract float padX(int i);

    protected abstract float padY(int i);

    /** 板 i 半径（或半宽）。 */
    protected abstract float padR(int i);

    /** 板 i 颜色。 */
    protected abstract int padColor(int i);

    /** 板形状：true 圆形，false 圆角方。 */
    protected boolean roundPads() {
        return true;
    }

    protected int padIndexAt(float x, float y) {
        for (int i = 0; i < padCount(); i++) {
            float dx = x - padX(i), dy = y - padY(i);
            float r = padR(i) + 6;
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }

    @Override
    public void restartGame() {
        level = 1;
        seqLen = 1;
        pads = new int[64];
        startRound();
    }

    private void startRound() {
        // 序列保留前缀，追加一个随机板
        pads[seqLen - 1] = randi(padCount());
        phase = SHOW;
        showIdx = 0;
        showUntil = 0;
        inputPos = 0;
        litPad = -1;
        host.setScore(level * 10 - 10);
        startLoop(90);
        dirty();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(90);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || phase != INPUT || action != 0) return;
        int p = padIndexAt(x, y);
        if (p < 0) return;
        litPad = p;
        beep(0);
        if (p == pads[inputPos]) {
            inputPos++;
            if (inputPos >= seqLen) {
                // 本轮完成
                beep(1);
                popup("第 " + level + " 关通过！", lw / 2f, 80, 0xFF7CFFB2, 18);
                burst(padX(p), padY(p), 0xFF7CFFB2, 12, 5f);
                level++;
                seqLen++;
                phase = SHOW;
                showIdx = 0;
                postDelayedSafe(this::startRound, 700);
            }
        } else {
            die();
            return;
        }
        dirty();
    }

    @Override
    protected void onTick() {
        long now = now();
        if (phase == SHOW) {
            if (now >= showUntil) {
                if (showIdx >= seqLen) {
                    phase = INPUT;
                    inputPos = 0;
                    litPad = -1;
                    popup("轮到你了", lw / 2f, 80, 0xFFECECF1, 14);
                } else {
                    litPad = pads[showIdx];
                    beep(1);
                    showUntil = now + 420; // 亮 420ms
                    showIdx++;
                }
            } else if (showUntil > 0 && now >= showUntil - 210) {
                // 亮 210ms 后熄灭间隙
                litPad = -1;
            }
        } else {
            litPad = -1;
        }
        dirty();
    }

    private void die() {
        over = true;
        stopLoop();
        shake(260);
        beep(2);
        popup("记错了！通过 " + (level - 1) + " 关", lw / 2f, 80, 0xFFE8574F, 16);
        int finalScore = Math.max(10, (level - 1) * 10);
        host.setScore(finalScore);
        dirty();
        postDelayedSafe(() -> host.gameOver(finalScore), 800);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF181C26);
        label(c, "等级 " + level + (phase == SHOW ? " · 看好了…" : " · 复现！"),
                lw / 2f, 36, 15, 0xFFECECF1, true);
        for (int i = 0; i < padCount(); i++) {
            boolean lit = i == litPad;
            int color = lit ? 0xFFFFFFFF : padColor(i);
            if (roundPads()) {
                fill.setColor(color);
                c.drawCircle(padX(i), padY(i), padR(i), fill);
            } else {
                float r = padR(i);
                rr(c, padX(i) - r, padY(i) - r, padX(i) + r, padY(i) + r, 12, color);
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
