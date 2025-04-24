package rasterizer;

import model.Point;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SelectionRasterizer {
    private final LineRasterizer lineRasterizer;

    public SelectionRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    public void drawSelectionBox(BufferedImage img, Point p1, Point p2) {
        Color[] colors = {Color.WHITE, Color.BLACK};

        int x1 = Math.min(p1.x, p2.x);
        int y1 = Math.min(p1.y, p2.y);
        int x2 = Math.max(p1.x, p2.x);
        int y2 = Math.max(p1.y, p2.y);

        // Dvě barvy pro mravence – střídají se mezi sebou
        for (int i = 0; i < colors.length; i++) {
            lineRasterizer.drawLine(img, new Point(x1 + i, y1), new Point(x2 + i, y1), colors[i], 1, null);
            lineRasterizer.drawLine(img, new Point(x2, y1 + i), new Point(x2, y2 + i), colors[i], 1, null);
            lineRasterizer.drawLine(img, new Point(x2 - i, y2), new Point(x1 - i, y2), colors[i], 1, null);
            lineRasterizer.drawLine(img, new Point(x1, y2 - i), new Point(x1, y1 - i), colors[i], 1, null);
        }
    }
}

