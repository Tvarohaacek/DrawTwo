package rasterizer;

import model.LineStyle;
import model.Point;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CircleRasterizer {

    /*
    * Vykreslí kruh s barvou, tloušťkou a stylem
    * Poloměr se vypočítává jako vzdálenost mezi dvěma zadanými body
    * Střed - první kliknutí
    * Bod okraje - místo, kam dragguju myší
    * Pro efekt tloušťky vykreslí více soustředných kruhů, jen mají menší poloměr*/
    public void drawCircle(BufferedImage img, Point center, Point edge, Color color, int thickness, LineStyle style) {
        int dx = edge.x - center.x;
        int dy = edge.y - center.y;
        int radius = (int) Math.round(Math.sqrt(dx * dx + dy * dy));

        for (int t = 0; t < thickness; t++) {
            drawStyledCircle(img, center.x, center.y, radius - t, color, style);
        }
    }

    /*Vykresluje kruh podle stylu, využívá logiku tzv patternů
    *Solid (default) - jednoduchý pattern {1} - pro každý krok se vykreslí
    *Dotted - Pattern {1,0,0,0} - vykreslí se jeden pixel, a 3 ne
    *Dashed - stejně jako u dotted, jen s větší mírou vykreslení */
    private void drawStyledCircle(BufferedImage img, int x0, int y0, int radius, Color color, LineStyle style) {
        int x = radius;
        int y = 0;
        int decisionOver2 = 1 - x;

        int[] pattern;
        switch (style) {
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

    /*Vykresluje body kružnice po osminách tak, že kontroluje hranici canvasu
    *Na body, které by vyšly mimo canvas bude podmínka false, tím pádem se ani nepokusí je kreslit
     */

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


