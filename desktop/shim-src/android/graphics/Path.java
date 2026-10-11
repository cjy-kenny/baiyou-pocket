package android.graphics;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** 桌面端 Path：Path2D 后端（含 winding 规则，供 EVEN_ODD 月牙等图形）。 */
public class Path {
    public enum Direction {CW, CCW}

    public enum FillType {WINDING, EVEN_ODD, INVERSE_WINDING, INVERSE_EVEN_ODD}

    final Path2D.Float p = new Path2D.Float();
    private FillType fillType = FillType.WINDING;

    public void moveTo(float x, float y) {
        p.moveTo(x, y);
    }

    public void lineTo(float x, float y) {
        p.lineTo(x, y);
    }

    public void cubicTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        p.curveTo(x1, y1, x2, y2, x3, y3);
    }

    public void close() {
        p.closePath();
    }

    public void addCircle(float cx, float cy, float r, Direction dir) {
        p.append(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2), false);
    }

    public void setFillType(FillType t) {
        this.fillType = t;
    }

    public FillType getFillType() {
        return fillType;
    }

    Path2D.Float path2d() {
        return p;
    }

    int windingRule() {
        return fillType == FillType.EVEN_ODD
                ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO;
    }
}
