package com.baiyou.pocket.desktop;

import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 游戏视图宿主面板：把 shim View 渲染到 Swing，并把鼠标事件转成触摸事件。
 *  缩放与居中由游戏内部（CanvasGame 逻辑坐标系）完成，本面板只给全尺寸画布。 */
public class ViewHostPanel extends JPanel {
    private final View view;

    public ViewHostPanel(View view) {
        this.view = view;
        setOpaque(true);
        // 游戏画面自成深色体系，letterbox 区域固定用深色（不随应用主题）
        setBackground(new Color(0x1B1E26));
        view.invalidateHook = this::repaint;
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                send(MotionEvent.ACTION_DOWN, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                send(MotionEvent.ACTION_UP, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                send(MotionEvent.ACTION_MOVE, e);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void send(int action, MouseEvent e) {
        view.performTouch(new MotionEvent(action, e.getX(), e.getY()));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (view.getWidth() != getWidth() || view.getHeight() != getHeight()) {
            view.performSizeChanged(getWidth(), getHeight(), view.getWidth(), view.getHeight());
        }
        Graphics2D g = (Graphics2D) g0.create();
        view.performDraw(new Canvas(g));
        g.dispose();
    }

    /** 演示模式注入：比例坐标 → 面板像素 → 触摸事件。 */
    public void inject(int action, float fx, float fy) {
        view.performTouch(new MotionEvent(action, fx * getWidth(), fy * getHeight()));
    }
}
