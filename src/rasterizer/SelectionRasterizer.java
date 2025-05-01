package rasterizer;

import model.*;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

public class SelectionRasterizer {
    private final RectangleRasterizer rectangleRasterizer;
    private final LineRasterizer lineRasterizer;

    public SelectionRasterizer() {
        this.lineRasterizer = new LineRasterizer();
        this.rectangleRasterizer = new RectangleRasterizer(lineRasterizer);
    }
    /* Rasterizer na výběr 
    * Funkcionalita - vyberu oblast a zkopíruju ji
    * */
        
    //Za Pomocí rectangle rasterizeru danou oblast ohraničí
    public void drawSelectionBox(BufferedImage canvas, Point start, Point end) {
        rectangleRasterizer.drawRectangle(canvas, start, end, java.awt.Color.CYAN, 1, LineStyle.DASHED, false);
    }

    //Vypočítá a vrátá objekt, který představuje ohraničovací rámeček s definicí dvou bodů
    //vytvoří ho bez ohledu na pořadí zadání bodů (k tomu ty absolutní hodnoty)
    public Rectangle getSelectionRect(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int width = Math.abs(end.x - start.x);
        int height = Math.abs(end.y - start.y);
        return new Rectangle(x, y, width, height);
    }

    //kontroluje, jestli daný bod leží v oblasti úchytu
    //vrací index příslušného rohu
    public int getHandleIndex(Point point, Rectangle rect) {

        if (isPointInHandleArea(point, rect.x, rect.y)) return 0;
        if (isPointInHandleArea(point, rect.x + rect.width, rect.y)) return 1;
        if (isPointInHandleArea(point, rect.x, rect.y + rect.height)) return 2;
        if (isPointInHandleArea(point, rect.x + rect.width, rect.y + rect.height)) return 3;

        return -1;
    }

    /*Představuje úchyt pro změnu velikosti a kontroluje, jestli je bod v oblasti úchytu*/
    private boolean isPointInHandleArea(Point point, int x, int y) {
        return point.x >= x - 10 && point.x <= x + 10 && point.y >= y - 10 && point.y <= y + 10;
    }

    /*Mění velikost výběru
    * parametry start a end definují původní rámeček
    * handle index určuje, za který roh výběru se táhne
    * logika switche mění souřadnice a rozměry obdélníku v závislosti
    * na tom, za který roh se zatáhne
    * kontrola záporné velikosti je také implementována v posletních dvouch if podmínkách*/

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
    //Nakresli každý roh jako malý křížek pomocí dvou čar
    public void drawHandles(BufferedImage canvas, Rectangle rect) {

        int size = 5;
        Point[] points = {
                new Point(rect.x, rect.y),
                new Point(rect.x + rect.width, rect.y),
                new Point(rect.x, rect.y + rect.height),
                new Point(rect.x + rect.width, rect.y + rect.height)
        };

        for (Point p : points) {
            //svislá čára
            lineRasterizer.drawLine(canvas,
                    new Point(p.x, p.y - size),
                    new Point(p.x, p.y + size),
                    java.awt.Color.CYAN, 1, LineStyle.SOLID);
            //vodorovná čára
            lineRasterizer.drawLine(canvas,
                    new Point(p.x - size, p.y),
                    new Point(p.x + size, p.y),
                    java.awt.Color.CYAN, 1, LineStyle.SOLID);
        }
    }
}
