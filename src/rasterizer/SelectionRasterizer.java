package rasterizer;

import model.Point;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SelectionRasterizer {

    // Metoda pro vykreslení výběru jako obdélníku
    public void drawSelectionBox(BufferedImage canvas, Point start, Point end) {
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.CYAN);
        g.setStroke(new BasicStroke(2));

        // Získání normalizovaného obdélníku (zajistí, že šířka a výška nejsou záporné)
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        g.drawRect(x, y, width, height);
        g.dispose();
    }

    // Metoda pro získání obdélníku výběru
    public Rectangle getSelectionRect(Point start, Point end) {
        // Normalizujeme souřadnice, aby šířka a výška nebyly záporné
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        return new Rectangle(x, y, width, height);
    }

    // Metoda pro zjištění, zda je bod v některém z úchytů výběru
    public int getHandleIndex(Point point, Rectangle rect) {
        final int HANDLE_RADIUS = 10;

        // Levý horní roh
        if (isPointInHandleArea(point, rect.x, rect.y, HANDLE_RADIUS)) return 0;
        // Pravý horní roh
        if (isPointInHandleArea(point, rect.x + rect.width, rect.y, HANDLE_RADIUS)) return 1;
        // Levý dolní roh
        if (isPointInHandleArea(point, rect.x, rect.y + rect.height, HANDLE_RADIUS)) return 2;
        // Pravý dolní roh
        if (isPointInHandleArea(point, rect.x + rect.width, rect.y + rect.height, HANDLE_RADIUS)) return 3;

        return -1; // Pokud bod není v žádném úchytu
    }

    // Metoda pro kontrolu, zda bod leží v okolí úchytu
    private boolean isPointInHandleArea(Point point, int x, int y, int radius) {
        return point.x >= x - radius && point.x <= x + radius && point.y >= y - radius && point.y <= y + radius;
    }

    // Metoda pro změnu velikosti výběru
    public Rectangle resizeSelection(Point start, Point end, int handleIndex, Point current) {
        int x = start.x;
        int y = start.y;
        int width = end.x - start.x;
        int height = end.y - start.y;

        // Určujeme, který úchyt je zvolen a měníme velikost výběru podle něj
        switch (handleIndex) {
            case 0: // Levý horní
                x = current.x;
                y = current.y;
                width = end.x - current.x;
                height = end.y - current.y;
                break;
            case 1: // Pravý horní
                width = current.x - start.x;
                y = current.y;
                height = end.y - current.y;
                break;
            case 2: // Levý dolní
                x = current.x;
                height = current.y - start.y;
                width = end.x - current.x;
                break;
            case 3: // Pravý dolní
                width = current.x - start.x;
                height = current.y - start.y;
                break;
        }

        // Normalizace obdélníku pro případ, že by šířka nebo výška byly záporné
        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }

        return new Rectangle(x, y, width, height);
    }

    // Opravená metoda pro přesunutí výběru


    // Metoda pro vykreslení úchytů pro změnu velikosti
    public void drawHandles(BufferedImage canvas, Rectangle rect) {
        final int HANDLE_RADIUS = 10;
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.CYAN);
        g.fillOval(rect.x - HANDLE_RADIUS / 2, rect.y - HANDLE_RADIUS / 2, HANDLE_RADIUS, HANDLE_RADIUS); // Levý horní
        g.fillOval(rect.x + rect.width - HANDLE_RADIUS / 2, rect.y - HANDLE_RADIUS / 2, HANDLE_RADIUS, HANDLE_RADIUS); // Pravý horní
        g.fillOval(rect.x - HANDLE_RADIUS / 2, rect.y + rect.height - HANDLE_RADIUS / 2, HANDLE_RADIUS, HANDLE_RADIUS); // Levý dolní
        g.fillOval(rect.x + rect.width - HANDLE_RADIUS / 2, rect.y + rect.height - HANDLE_RADIUS / 2, HANDLE_RADIUS, HANDLE_RADIUS); // Pravý dolní
        g.dispose();
    }
}