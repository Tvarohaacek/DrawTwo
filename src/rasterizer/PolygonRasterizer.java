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


    /* Polygon - List bodů
    * Klikáním na rastr volím body, do kterých povedou čáry
    * V listu se ukládají souřadnice bodů, ze kterých se potom
    * forcyklem bere předposlední a rýsuje se k poslednímu
    * Polygon se zavře pouze pokud kliknu k jeho počátku (isCloseToFirst)
    *  */
    public PolygonRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }
    //přidá bod do listu
    public void addPoint(Point p) {
        points.add(p);
    }

    //kontroluje blízkost a uzavírá
    public boolean isCloseToFirst(Point p, int tolerance) {
        if (points.isEmpty()) return false;
        Point first = points.getFirst();
        int dx = p.x - first.x;
        int dy = p.y - first.y;
        return dx * dx + dy * dy <= tolerance * tolerance;
    }
    //kreslí podle forcyklů - opět využívá logiku čar, aby se polygon nemusel dělat zvlášť
    public void drawPolygon(BufferedImage img, Color color, int thickness, LineStyle style, boolean close) {
        for (int i = 1; i < points.size(); i++) {
            lineRasterizer.drawLine(img, points.get(i - 1), points.get(i), color, thickness, style);
        }
        if (close && points.size() > 2) {
            lineRasterizer.drawLine(img, points.getLast(), points.getFirst(), color, thickness, style);
        }
    }

    public void clear() {
        points.clear();
    }
    //vyčístí list bodů

    public List<Point> getPoints() {
        return points;
    }
    //getter metoda

    public boolean isEmpty() {
        return points.isEmpty();
    }
    //checker metoda
}

