package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g085 炮台角度：拖动调仰角（虚线显示弹道预测），开炮轰击远近变化的堡垒；
 *  命中按剩余弹药数给分，五发打完结算。 */
public class CannonAngleGame extends CanvasGame {
    private final Random rnd = new Random();
    private float fortX = 290;
    private float angle = 55;     // 仰角（度）
    private float projX, projY, projVx, projVy;
    private boolean flying;
    private int shots = 5, score;
    private boolean dragAim;
    private boolean settling;

    public CannonAngleGame() {
        super(360, 560);
    }

    private void startRound() {
        score = 0;
        shots = 5;
        fortX = 260 + rnd.nextInt(60);
        flying = false;
        dragAim = false;
        settling = false;
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

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling) return;
        if (action == 0 && !flying && y > 300 && x < 150) {
            dragAim = true;
            angle = 55;
        } else if (action == 2 && dragAim) {
            // 拖得越靠上角度越大
            angle = Math.max(20f, Math.min(80f, (300 - y) * 0.5f));
        } else if (action == 1 && dragAim) {
            dragAim = false;
            if (shots > 0) {
                shots--;
                projX = 60;
                projY = 470;
                double a = Math.toRadians(angle);
                projVx = (float) Math.cos(a) * 12.5f;
                projVy = -(float) Math.sin(a) * 12.5f;
                flying = true;
                beep(0);
            }
        }
        dirty();
    }

    private void finish(String why) {
        settling = true;
        over = true;
        stopLoop();
        popup(why + " 得分 " + score, lw / 2f, lh / 2f, 0xFFECECF1, 18);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 900);
    }

    @Override
    protected void onTick() {
        if (flying) {
            projVy += 0.15f;
            projX += projVx;
            projY += projVy;
            // 堡垒命中（任意高度命中堡垒区域）
            if (projX > fortX - 24 && projX < fortX + 24 && projY > 380) {
                int gain = 100 + shots * 30;
                score += gain;
                host.setScore(score);
                beep(1);
                burst(fortX, 400, 0xFFE8574F, 18, 7f);
                popup("命中！ +" + gain, fortX, 370, 0xFFFFC24B, 18);
                flying = false;
                fortX = 240 + rnd.nextInt(80);
            }
            if (projX > lw + 20 || projY > 530) {
                flying = false;
                beep(2);
            }
        }
        if (shots <= 0 && !flying) {
            finish("炮弹打完");
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF2A2418);
        rr(c, 0, 470, lw, lh - 470, 0, 0xFF5C5030);
        // 堡垒
        fill.setColor(0xFF6B7280);
        rr(c, fortX - 24, 380, fortX + 24, 470, 4, 0xFF6B7280);
        fill.setColor(0xFF4A5568);
        rr(c, fortX - 30, 366, fortX + 30, 382, 3, 0xFF4A5568);
        // 炮台
        fill.setColor(0xFF4A5568);
        c.drawCircle(60, 470, 20, fill);
        double a = Math.toRadians(-angle);
        stroke.setColor(0xFFECECF1);
        stroke.setStrokeWidth(6);
        c.drawLine(60, 470, 60 + (float) Math.cos(a) * 36, 470 + (float) Math.sin(a) * 36, stroke);
        // 角度标签
        label(c, (int) angle + "°", 60, 430, 14, 0xFF9AA0AC, true);
        // 弹道预测（拖拽时）
        if (dragAim && !flying) {
            double rad = Math.toRadians(angle);
            float px2 = 60, py2 = 470;
            float pvx = (float) Math.cos(rad) * 12.5f, pvy = -(float) Math.sin(rad) * 12.5f;
            stroke.setColor(0x887CFFB2);
            stroke.setStrokeWidth(2);
            for (int i = 0; i < 40; i++) {
                pvy += 0.15f;
                px2 += pvx;
                py2 += pvy;
                if (py2 > 530 || px2 > lw) break;
                c.drawCircle(px2, py2, 2, stroke);
            }
        }
        if (flying) {
            fill.setColor(0xFF1B1C22);
            c.drawCircle(projX, projY, 7, fill);
        }
        label(c, "剩余 " + shots, lw - 44, 32, 15, 0xFFC9B896, true);
        label(c, "拖动下方调仰角", lw / 2f, 530, 12, 0xFF9AA0AC, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
