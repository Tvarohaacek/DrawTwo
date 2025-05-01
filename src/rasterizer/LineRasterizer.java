package rasterizer;

import model.Point;
import model.LineStyle;
import java.awt.image.BufferedImage;
import java.awt.Color;


public class LineRasterizer {

    /*
    * Mezi danými souřadnicemi vykreslí čáru se stylem
    * a tloušťkou. Styl a tloušťka jsou proporcionální
    * v závislosti na tloušťce čáry se mění proporce teček
    * a čar.
    *
    * Čára se kreslí přes while loop - Bresenhamův algoritmus
    * 1. Vykreslí se pixel v aktuální poloze
    * 2. zkontroluje se, jestli se dosáhlo koncového bodu
    * 3. Vypočítá se, kam se posunout v dalším kroku
    * https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    *
    * Jednoduché vysvětlení:
    *
    * podle toho, jestli je čára více vodorovná nebo svislá
    * se vypočítává, kterým směrem se posunout
    * Vedlejším směrem se algoritmus vydá tehdy, kdy je odchylka
    * v algoritmu dostatečně velká na to, aby se zohlednila jako
    * směr přímky
    * */

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

    //Bere velikost pixelu v ohledu na hranice canvasu


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
    /*Pomocí goniometrických funkcí (atan2) prorovnává
    úhel mezi body, a pak zaokrouhlí body na stejnou Xovou
    nebo Ypsilonovou souřadnici
     */

    public Point snapTo45Degrees(Point start, Point current) {
        double dx = current.x - start.x;
        double dy = current.y - start.y;

        double angle = Math.atan2(dy, dx);
        double degree = Math.toDegrees(angle);


        if (degree < 0) {
            degree += 360;
        }


        int snappedDegree = (int) (Math.round(degree / 45.0) * 45) % 360;

        double rad = Math.toRadians(snappedDegree);
        double length = Math.hypot(dx, dy);

        int snappedX = (int) Math.round(start.x + Math.cos(rad) * length);
        int snappedY = (int) Math.round(start.y + Math.sin(rad) * length);

        return new Point(snappedX, snappedY);
    }

}
