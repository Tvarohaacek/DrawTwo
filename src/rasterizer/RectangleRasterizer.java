package rasterizer;

import model.LineStyle;
import java.awt.*;
import java.awt.image.BufferedImage;
import model.*;
import model.Point;

public class RectangleRasterizer {

    private final LineRasterizer lineRasterizer;

    public RectangleRasterizer(LineRasterizer lineRasterizer) {
        this.lineRasterizer = lineRasterizer;
    }

    /*
    * Na základě znalosti úhlopříčky narýsuje obdélník
    * 1. Určí všechny 4 body předáním souřadnic rohů
    * 2. Zjistí, jestli se jendá o čtverec (pokud ano, osekne stranu)
    * 3. Zajistí okraje canvasu
    * 4. Pomocí line rasterizeru vykreslí čáry samostatně z jednotlivých bodů
    * Při držení shiftu rýsuje čtverec, */

    public void drawRectangle(BufferedImage img, Point p1, Point p2, Color color, int thickness, LineStyle style, boolean squareMode) {

        int x1 = p1.x;
        int y1 = p1.y;
        int x2 = p2.x;
        int y2 = p2.y;


        if (squareMode) {
            int size = Math.min(Math.abs(x2 - x1), Math.abs(y2 - y1));
            x2 = x1 + (x2 < x1 ? -size : size);
            y2 = y1 + (y2 < y1 ? -size : size);
        }


        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);


        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        maxX = Math.min(maxX, img.getWidth() - 1);
        maxY = Math.min(maxY, img.getHeight() - 1);


        Point topLeft     = new Point(minX, minY);
        Point topRight    = new Point(maxX, minY);
        Point bottomLeft  = new Point(minX, maxY);
        Point bottomRight = new Point(maxX, maxY);


        lineRasterizer.drawLine(img, topLeft, topRight, color, thickness, style);
        lineRasterizer.drawLine(img, topRight, bottomRight, color, thickness, style);
        lineRasterizer.drawLine(img, bottomRight, bottomLeft, color, thickness, style);
        lineRasterizer.drawLine(img, bottomLeft, topLeft, color, thickness, style);
    }
}
