package rasterizer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;

import model.Point;

public class FillRasterizer {

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

