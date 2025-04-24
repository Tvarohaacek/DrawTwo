package rasterizer;

import model.Point;
import model.LineStyle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PolygonRasterizer {
    private final LineRasterizer lineRasterizer;
    private final List<Point> points = new ArrayList<>();

    public PolygonRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    public void addPoint(Point p) {
        points.add(p);
    }

    public boolean isCloseToFirst(Point p, int tolerance) {
        if (points.isEmpty()) return false;
        Point first = points.get(0);
        int dx = p.x - first.x;
        int dy = p.y - first.y;
        return dx * dx + dy * dy <= tolerance * tolerance;
    }

    public void drawPolygon(BufferedImage img, Color color, int thickness, LineStyle style, boolean close) {
        for (int i = 1; i < points.size(); i++) {
            lineRasterizer.drawLine(img, points.get(i - 1), points.get(i), color, thickness, style);
        }
        if (close && points.size() > 2) {
            lineRasterizer.drawLine(img, points.get(points.size() - 1), points.get(0), color, thickness, style);
        }
    }

    public void clear() {
        points.clear();
    }

    public List<Point> getPoints() {
        return points;
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
}

