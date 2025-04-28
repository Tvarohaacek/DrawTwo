package rasterizer;

import model.Point;
import model.LineStyle;
import java.awt.image.BufferedImage;
import java.awt.Color;


public class LineRasterizer {

    public void drawLine(BufferedImage img, Point p1, Point p2, Color color, int thickness, LineStyle style) {
        int x0 = p1.x;
        int y0 = p1.y;
        int x1 = p2.x;
        int y1 = p2.y;

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int step = 0;
        int dashLength = thickness * 3;
        int spaceLength = style == LineStyle.DOTTED ? thickness * 2 : thickness * 3;

        while (true) {
            boolean draw = true;

            if (style == LineStyle.DASHED) {
                int cycle = dashLength + spaceLength;
                draw = (step % cycle) < dashLength;
            } else if (style == LineStyle.DOTTED) {
                int cycle = thickness + spaceLength;
                draw = (step % cycle) < thickness;
            }

            if (draw) {
                drawThickPixel(img, x0, y0, thickness, color);
            }

            if (x0 == x1 && y0 == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }

            step++;
        }
    }

    private void drawThickPixel(BufferedImage img, int x, int y, int thickness, Color color) {
        int radius = thickness / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && py >= 0 && px < img.getWidth() && py < img.getHeight()) {
                    img.setRGB(px, py, color.getRGB());
                }
            }
        }
    }

    public Point snapTo45Degrees(Point start, Point current) {
        double dx = current.x - start.x;
        double dy = current.y - start.y;

        double angle = Math.atan2(dy, dx); // Úhel v radiánech
        double degree = Math.toDegrees(angle);

        // Normalizace na [0, 360)
        if (degree < 0) {
            degree += 360;
        }

        // Najdeme nejbližší násobek 45
        int snappedDegree = (int) (Math.round(degree / 45.0) * 45) % 360;

        double rad = Math.toRadians(snappedDegree);
        double length = Math.hypot(dx, dy);

        int snappedX = (int) Math.round(start.x + Math.cos(rad) * length);
        int snappedY = (int) Math.round(start.y + Math.sin(rad) * length);

        return new Point(snappedX, snappedY);
    }

}
