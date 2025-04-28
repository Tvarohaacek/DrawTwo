package rasterizer;

import model.LineStyle;
import model.Point;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CircleRasterizer {
    public void drawCircle(BufferedImage img, Point center, Point edge, Color color, int thickness, LineStyle style) {
        int dx = edge.x - center.x;
        int dy = edge.y - center.y;
        int radius = (int) Math.round(Math.sqrt(dx * dx + dy * dy));

        for (int t = 0; t < thickness; t++) {
            drawStyledCircle(img, center.x, center.y, radius - t, color, style);
        }
    }

    private void drawStyledCircle(BufferedImage img, int x0, int y0, int radius, Color color, LineStyle style) {
        int x = radius;
        int y = 0;
        int decisionOver2 = 1 - x; // Rozhodovací proměnná v algoritmu

        int[] pattern;
        switch (style) {
            case SOLID -> pattern = new int[]{1};
            case DOTTED -> pattern = new int[]{1, 0, 0, 0};
            case DASHED -> pattern = new int[]{1, 1, 1, 1, 0, 0, 0, 0};
            default -> pattern = new int[]{1};
        }
        int patternIndex = 0;
        int patternLength = pattern.length;

        while (y <= x) {
            if (pattern[patternIndex % patternLength] == 1) {
                plotCirclePoints(img, x0, y0, x, y, color);
            }
            patternIndex++;

            y++;
            if (decisionOver2 <= 0) {
                decisionOver2 += 2 * y + 1;
            } else {
                x--;
                decisionOver2 += 2 * (y - x) + 1;
            }
        }
    }


    private void plotCirclePoints(BufferedImage img, int cx, int cy, int x, int y, Color color) {
        int[][] points = {
                {cx + x, cy + y}, {cx + y, cy + x},
                {cx - y, cy + x}, {cx - x, cy + y},
                {cx - x, cy - y}, {cx - y, cy - x},
                {cx + y, cy - x}, {cx + x, cy - y},
        };

        for (int[] p : points) {
            int px = p[0], py = p[1];
            if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                img.setRGB(px, py, color.getRGB());
            }
        }
    }
}


