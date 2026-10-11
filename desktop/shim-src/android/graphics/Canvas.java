package android.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;

/** 桌面端 Canvas：把 android.graphics.Canvas 的游戏绘制面映射到 java.awt.Graphics2D。
 *  仅实现游戏代码实际用到的子集（save/restore/translate/scale + 圆角矩形/圆/路径/弧/文字）。 */
public class Canvas {
    private final Graphics2D g;
    private final ArrayDeque<AffineTransform> stack = new ArrayDeque<>();

    public Canvas(Graphics2D g) {
        this.g = g;
    }

    public int save() {
        stack.push(g.getTransform());
        return stack.size();
    }

    public void restore() {
        if (!stack.isEmpty()) g.setTransform(stack.pop());
    }

    public void translate(float dx, float dy) {
        g.translate(dx, dy);
    }

    public void scale(float sx, float sy) {
        g.scale(sx, sy);
    }

    public void drawRoundRect(RectF r, float rx, float ry, Paint p) {
        rx = Math.min(rx, Math.min(r.width(), r.height()) / 2f);
        shape(new RoundRectangle2D.Float(r.left, r.top, r.width(), r.height(), rx * 2, ry * 2), p);
    }

    public void drawCircle(float cx, float cy, float radius, Paint p) {
        shape(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2), p);
    }

    public void drawLine(float x1, float y1, float x2, float y2, Paint p) {
        applyAA(p);
        g.setColor(new Color(p.getColor(), true));
        float w = Math.max(0.1f, p.getStrokeWidth());
        int cap = p.getStrokeCap() == Paint.Cap.ROUND
                ? BasicStroke.CAP_ROUND : BasicStroke.CAP_BUTT;
        g.setStroke(new BasicStroke(w, cap, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Float(x1, y1, x2, y2));
    }

    public void drawPath(Path path, Paint p) {
        Path2D.Float shaped = new Path2D.Float(path.windingRule());
        shaped.append(path.path2d(), false);
        shape(shaped, p);
    }

    public void drawArc(RectF r, float startAngle, float sweepAngle, boolean useCenter, Paint p) {
        shape(new Arc2D.Float(r.left, r.top, r.width(), r.height(),
                startAngle, sweepAngle, useCenter ? Arc2D.PIE : Arc2D.OPEN), p);
    }

    public void drawText(String text, float x, float y, Paint p) {
        applyAA(p);
        Font f = ShimFont.font(p);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics(f);
        float w = fm.stringWidth(text);
        float dx = x;
        if (p.getTextAlign() == Paint.Align.CENTER) dx = x - w / 2f;
        else if (p.getTextAlign() == Paint.Align.RIGHT) dx = x - w;
        g.setColor(color(p));
        g.drawString(text, dx, y);
    }

    private void shape(Shape s, Paint p) {
        applyAA(p);
        g.setColor(color(p));
        Paint.Style st = p.getStyle();
        if (st == Paint.Style.FILL || st == Paint.Style.FILL_AND_STROKE) {
            g.fill(s);
        }
        if (st == Paint.Style.STROKE || st == Paint.Style.FILL_AND_STROKE) {
            float w = Math.max(0.1f, p.getStrokeWidth());
            int cap = p.getStrokeCap() == Paint.Cap.ROUND
                    ? BasicStroke.CAP_ROUND : BasicStroke.CAP_BUTT;
            g.setStroke(new BasicStroke(w, cap, BasicStroke.JOIN_ROUND));
            g.draw(s);
        }
    }

    private static Color color(Paint p) {
        return new Color(p.getColor(), true);
    }

    private void applyAA(Paint p) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                p.isAntiAlias() ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
