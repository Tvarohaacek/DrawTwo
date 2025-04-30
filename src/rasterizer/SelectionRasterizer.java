package rasterizer;

import model.Point;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SelectionRasterizer {


    public void drawSelectionBox(BufferedImage canvas, Point start, Point end) {
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.CYAN);
        g.setStroke(new BasicStroke(2));


        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        g.drawRect(x, y, width, height);
        g.dispose();
    }


    public Rectangle getSelectionRect(Point start, Point end) {

        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);

        return new Rectangle(x, y, width, height);
    }


    public int getHandleIndex(Point point, Rectangle rect) {
        final int HANDLE_RADIUS = 10;


        if (isPointInHandleArea(point, rect.x, rect.y)) return 0;

        if (isPointInHandleArea(point, rect.x + rect.width, rect.y)) return 1;

        if (isPointInHandleArea(point, rect.x, rect.y + rect.height)) return 2;

        if (isPointInHandleArea(point, rect.x + rect.width, rect.y + rect.height)) return 3;

        return -1;
    }


    private boolean isPointInHandleArea(Point point, int x, int y) {
        return point.x >= x - 10 && point.x <= x + 10 && point.y >= y - 10 && point.y <= y + 10;
    }


    public Rectangle resizeSelection(Point start, Point end, int handleIndex, Point current) {
        int x = start.x;
        int y = start.y;
        int width = end.x - start.x;
        int height = end.y - start.y;


        switch (handleIndex) {
            case 0:
                x = current.x;
                y = current.y;
                width = end.x - current.x;
                height = end.y - current.y;
                break;
            case 1:
                width = current.x - start.x;
                y = current.y;
                height = end.y - current.y;
                break;
            case 2:
                x = current.x;
                height = current.y - start.y;
                width = end.x - current.x;
                break;
            case 3:
                width = current.x - start.x;
                height = current.y - start.y;
                break;
        }


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