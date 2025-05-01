package rasterizer;

import model.Point;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BrushRasterizer {

    //Vykreslí bod (kam, souřadnice, tloušťka, barva)
    public void drawPoint(BufferedImage img, Point point, int thickness, Color color) {
        ThickPix(img, point.x, point.y, thickness, color);
    }

    //Vykresluje barevný čtverec pixelů kolem daného bodu s konkrétní tloušťkou a to tak,
    //aby kontrolovala hranici obrázku
    private void ThickPix(BufferedImage img, int x, int y, int thickness, Color color) {
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
}
