package rasterizer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;
import model.Point;

public class FillRasterizer {

    /*
    * Používá Flood Fill algoritmus, podrobný popis lze nalézt zde
    * https://en.wikipedia.org/wiki/Flood_fill
    * Používám možnost s frontou - při použití fronty (nebo zásobníku)
    * se předejde častému problému stack overflow
    * Do fronty se řadí jednotlivé pixely, ty se zabarví
    * z queue se odstraní a přidají se sousední
    *
    * Chytré je na tom vlastně to, že se pixely plní jen tehdy, když je fronta prázdná,
    * (tj. když nemá jaký bod kontrolovat),
    * tím nedochází k tomu přehlcení a pracuje se s minimem paměti*/


    public void floodFill(BufferedImage canvas, Point start, Color fillColor) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int targetColor = canvas.getRGB(start.x, start.y);
        int replacementColor = fillColor.getRGB();

        if (targetColor == replacementColor) return;

        Queue<Point> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int x = p.x;
            int y = p.y;

            if (x < 0 || x >= width || y < 0 || y >= height) continue;
            if (canvas.getRGB(x, y) != targetColor) continue;

            canvas.setRGB(x, y, replacementColor);

            queue.add(new Point(x + 1, y));
            queue.add(new Point(x - 1, y));
            queue.add(new Point(x, y + 1));
            queue.add(new Point(x, y - 1));
        }
    }
}

