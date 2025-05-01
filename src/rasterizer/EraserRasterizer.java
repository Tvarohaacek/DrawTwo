package rasterizer;

import model.Point;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class EraserRasterizer {

    private static final Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;
/*
* Stejné jako BrushRasterizer, pouze vše maže na defaultní barvu,
* kterou je v mém případě černá
* */
    public void erasePoint(BufferedImage img, Point point, int thickness) {
        drawThickPixel(img, point.x, point.y, thickness);
    }

    private void drawThickPixel(BufferedImage img, int x, int y, int thickness) {
        int radius = thickness / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= 0 && py >= 0 && px < img.getWidth() && py < img.getHeight()) {
                    img.setRGB(px, py, EraserRasterizer.DEFAULT_BACKGROUND_COLOR.getRGB());
                }
            }
        }
    }
}

