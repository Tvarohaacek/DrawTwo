package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import model.LineStyle;
import model.Point;
import model.ToolType;
import rasterizer.*;

import java.util.List;

public class DrawingPanel extends JPanel {
    private BufferedImage canvas;
    private BufferedImage temp;
    private BufferedImage backupCanvas;
    private Point start;
    private Point currentMouse;
    private final LineRasterizer lineRasterizer;
    private final RectangleRasterizer rectangleRasterizer;
    private final CircleRasterizer circleRasterizer;
    private final PolygonRasterizer polygonRasterizer;
    private final FillRasterizer fillRasterizer;
    private final SelectionRasterizer selectionRasterizer;
    private final BrushRasterizer brushRasterizer = new BrushRasterizer();
    private final EraserRasterizer eraserRasterizer = new EraserRasterizer();


    private Color currentColor = Color.WHITE;
    private int currentThickness = 1;
    private LineStyle currentStyle = LineStyle.SOLID;
    private ToolType currentTool = ToolType.LINE;
    private Point lastMousePoint = null;
    private ToolType lastShapeType = null;
    private Point lastShapeStart = null;
    private Point lastShapeEnd = null;
    private int selectedHandle = -1;
    private boolean isEditingLastShape = false;



    private Point selectionStart, selectionEnd;
    private BufferedImage selectedImage;
    private boolean isDraggingSelection = false;
    private int selectionHandleIndex = -1;
    private Point dragOffset;

    public DrawingPanel(int width, int height) {
        this.setPreferredSize(new Dimension(width, height));
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        backupCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        lineRasterizer = new LineRasterizer();
        rectangleRasterizer = new RectangleRasterizer(lineRasterizer);
        circleRasterizer = new CircleRasterizer();
        polygonRasterizer = new PolygonRasterizer(lineRasterizer);
        fillRasterizer = new FillRasterizer();
        selectionRasterizer = new SelectionRasterizer();

        clearCanvas();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
                currentMouse = p;
                start = p;

                if (currentTool == ToolType.BRUSH || currentTool == ToolType.ERASER) {
                    lastMousePoint = p;
                }

                if (lastShapeType != null) {
                    Point handle1 = lastShapeStart;
                    Point handle2 = lastShapeEnd;
                    Point center = new Point((handle1.x + handle2.x) / 2, (handle1.y + handle2.y) / 2);

                    if (isOnHandle(p, handle1)) {
                        selectedHandle = 0;
                        isEditingLastShape = true;
                        clearTemp();
                        return;
                    } else if (isOnHandle(p, handle2)) {
                        selectedHandle = 1;
                        isEditingLastShape = true;
                        clearTemp();
                        return;
                    } else if (isOnHandle(p, center)) {
                        selectedHandle = 2;
                        dragOffset = new Point(p.x - center.x, p.y - center.y);
                        isEditingLastShape = true;
                        clearTemp();
                        return;
                    }
                }


                switch (currentTool) {
                    case POLYGON -> {if (!polygonRasterizer.isEmpty() && polygonRasterizer.isCloseToFirst(p, 10)) {
                        polygonRasterizer.drawPolygon(canvas, currentColor, currentThickness, currentStyle, true);
                        polygonRasterizer.clear();
                    } else {
                        polygonRasterizer.addPoint(p);
                    }}
                    case SELECTION ->
                    {
                        Rectangle rect = selectionStart != null && selectionEnd != null
                                    ? selectionRasterizer.getSelectionRect(selectionStart, selectionEnd) : null;
                            if (rect != null) {
                                selectionHandleIndex = selectionRasterizer.getHandleIndex(p, rect);
                                if (selectionHandleIndex != -1) return;

                                if (rect.contains(new java.awt.Point(p.x, p.y))) {
                                    isDraggingSelection = true;
                                    dragOffset = new Point(p.x - rect.x, p.y - rect.y);
                                    return;
                                }
                            }

                            backupCurrentCanvas();
                            selectionStart = p;
                            selectionEnd = p;
                            selectedImage = null;
                    }
                    case BRUSH -> {
                        brushRasterizer.drawPoint(canvas, start, currentThickness, currentColor);
                        repaint();
                    }
                    case ERASER -> {
                        eraserRasterizer.erasePoint(canvas, start, currentThickness);
                        repaint();
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point end = new Point(e.getX(), e.getY());
                boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                if (currentTool == ToolType.BRUSH || currentTool == ToolType.ERASER) {
                    lastMousePoint = null;
                }

                if (isDraggingSelection) {

                    applySelection();
                    isDraggingSelection = false;
                    repaint();
                    return;
                }

                if (isEditingLastShape) {
                    clearTemp(); // smažeme temp plátno
                    drawLastShapeToCanvas(); // nakreslíme nový tvar napevno
                    isEditingLastShape = false;
                    selectedHandle = -1;
                    repaint();
                    return;
                }


                if (selectionHandleIndex != -1) {

                    Rectangle rect = selectionRasterizer.resizeSelection(selectionStart, selectionEnd, selectionHandleIndex, end);
                    selectionStart = new Point(rect.x, rect.y);
                    selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
                    selectionHandleIndex = -1;


                    updateSelectedImage();
                    repaint();
                    return;
                }

                if (currentTool == ToolType.SELECTION) {
                    selectionEnd = end;
                    updateSelectedImage();
                    repaint();
                    return;
                }

                switch (currentTool) {
                    case LINE -> {
                        lineRasterizer.drawLine(canvas, start, end, currentColor, currentThickness, currentStyle);
                            lastShapeType = currentTool;
                            lastShapeStart = start;
                            lastShapeEnd = end;
                        if (isEditingLastShape) {
                            clearTemp();
                            drawLastShapeToTemp();
                            repaint();
                            return;
                        }

                    }
                    case RECTANGLE -> {
                        rectangleRasterizer.drawRectangle(canvas, start, end, currentColor, currentThickness, currentStyle, shift);
                        lastShapeType = currentTool;
                        lastShapeStart = start;
                        lastShapeEnd = end;
                        if (isEditingLastShape) {
                            clearTemp();
                            drawLastShapeToTemp();  // vykreslí aktuální upravovaný tvar
                            repaint();
                            return;
                        }

                    }
                    case CIRCLE -> {
                        circleRasterizer.drawCircle(canvas, start, end, currentColor, currentThickness, currentStyle);
                        lastShapeType = currentTool;
                        lastShapeStart = start;
                        lastShapeEnd = end;
                        if (isEditingLastShape) {
                            clearTemp();
                            drawLastShapeToTemp();  // vykreslí aktuální upravovaný tvar
                            repaint();
                            return;
                        }

                    }
                    case FILL -> fillRasterizer.floodFill(canvas, end, currentColor);


                }



                clearTemp();
                start = null;
                currentMouse = null;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point dragged = new Point(e.getX(), e.getY());
                currentMouse = dragged;
                boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                clearTemp();

                if (selectedHandle != -1 && lastShapeStart != null && lastShapeEnd != null) {
                    switch (selectedHandle) {
                        case 0 -> lastShapeStart = dragged;
                        case 1 -> lastShapeEnd = dragged;
                        case 2 -> {
                            int dx = dragged.x - dragOffset.x - (lastShapeStart.x + lastShapeEnd.x) / 2;
                            int dy = dragged.y - dragOffset.y - (lastShapeStart.y + lastShapeEnd.y) / 2;
                            lastShapeStart = new Point(lastShapeStart.x + dx, lastShapeStart.y + dy);
                            lastShapeEnd = new Point(lastShapeEnd.x + dx, lastShapeEnd.y + dy);
                        }
                    }
                    clearTemp();
                    drawLastShapeToTemp();
                    repaint();
                    currentMouse = null;
                    start = null;
                    return;

                }

                if (isDraggingSelection && dragOffset != null) {
                    Point newTopLeft = new Point(dragged.x - dragOffset.x, dragged.y - dragOffset.y);
                    selectionStart = newTopLeft;
                    selectionEnd = new Point(newTopLeft.x + selectedImage.getWidth(), newTopLeft.y + selectedImage.getHeight());
                    repaint();
                    return;
                }

                if (selectionHandleIndex != -1 && selectionStart != null && selectionEnd != null) {
                    Rectangle rect = selectionRasterizer.resizeSelection(selectionStart, selectionEnd, selectionHandleIndex, dragged);
                    selectionStart = new Point(rect.x, rect.y);
                    selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
                    repaint();
                    return;
                }


                switch (currentTool) {
                    case LINE -> lineRasterizer.drawLine(temp, start, dragged, currentColor, currentThickness, currentStyle);
                    case RECTANGLE -> rectangleRasterizer.drawRectangle(temp, start, dragged, currentColor, currentThickness, currentStyle, shift);
                    case CIRCLE -> circleRasterizer.drawCircle(temp, start, dragged, currentColor, currentThickness, currentStyle);
                    case SELECTION -> {
                        selectionEnd = dragged;
                        repaint();
                        return;
                    }
                    case BRUSH -> {

                        if (lastMousePoint != null) {
                            drawInterpolatedBrushLine(lastMousePoint, dragged);
                        } else {
                            brushRasterizer.drawPoint(canvas, dragged, currentThickness, currentColor);
                        }
                        lastMousePoint = dragged;
                        start = dragged;
                    }
                    case ERASER -> {

                        if (lastMousePoint != null) {
                            drawInterpolatedEraserLine(lastMousePoint, dragged);
                        } else {
                            eraserRasterizer.erasePoint(canvas, dragged, currentThickness);
                        }
                        lastMousePoint = dragged;
                        start = dragged;
                    }
                }
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                currentMouse = new Point(e.getX(), e.getY());
                repaint();
            }
        };

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
    }

    private void drawInterpolatedBrushLine(Point from, Point to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        for (int i = 0; i <= steps; i++) {
            int x = from.x + i * dx / steps;
            int y = from.y + i * dy / steps;
            brushRasterizer.drawPoint(canvas, new Point(x, y), currentThickness, currentColor);
        }
    }

    private void drawLastShapeToTemp() {
        if (lastShapeType == null || lastShapeStart == null || lastShapeEnd == null) return;

        switch (lastShapeType) {
            case LINE -> lineRasterizer.drawLine(temp, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
            case RECTANGLE -> rectangleRasterizer.drawRectangle(temp, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle, false);
            case CIRCLE -> circleRasterizer.drawCircle(temp, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
        }
    }

    private void drawLastShapeToCanvas() {
        if (lastShapeType == null || lastShapeStart == null || lastShapeEnd == null) return;

        switch (lastShapeType) {
            case LINE -> lineRasterizer.drawLine(canvas, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
            case RECTANGLE -> rectangleRasterizer.drawRectangle(canvas, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle, false);
            case CIRCLE -> circleRasterizer.drawCircle(canvas, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
        }
    }



    private void drawInterpolatedEraserLine(Point from, Point to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));

        for (int i = 0; i <= steps; i++) {
            int x = from.x + i * dx / steps;
            int y = from.y + i * dy / steps;
            eraserRasterizer.erasePoint(canvas, new Point(x, y), currentThickness);
        }
    }


    private void backupCurrentCanvas() {

        Graphics g = backupCanvas.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
    }


    private void updateSelectedImage() {
        if (selectionStart == null || selectionEnd == null) return;


        Rectangle rect = getNormalizedSelectionRect();


        if (rect.width <= 0 || rect.height <= 0) {
            selectedImage = null;
            return;
        }


        rect.x = Math.max(0, rect.x);
        rect.y = Math.max(0, rect.y);
        rect.width = Math.min(canvas.getWidth() - rect.x, rect.width);
        rect.height = Math.min(canvas.getHeight() - rect.y, rect.height);

        if (rect.width <= 0 || rect.height <= 0) {
            selectedImage = null;
            return;
        }


        selectedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);


        for (int y = 0; y < rect.height; y++) {
            for (int x = 0; x < rect.width; x++) {
                int rgb = canvas.getRGB(rect.x + x, rect.y + y);
                selectedImage.setRGB(x, y, rgb);
            }
        }


        selectionStart = new Point(rect.x, rect.y);
        selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
    }


    private Rectangle getNormalizedSelectionRect() {
        int x = Math.min(selectionStart.x, selectionEnd.x);
        int y = Math.min(selectionStart.y, selectionEnd.y);
        int width = Math.abs(selectionEnd.x - selectionStart.x);
        int height = Math.abs(selectionEnd.y - selectionStart.y);

        return new Rectangle(x, y, width, height);
    }

    private Rectangle getHandleRect(Point p) {
        int size = 6;
        return new Rectangle(p.x - size / 2, p.y - size / 2, size, size);
    }

    private boolean isOnHandle(Point mouse, Point handle) {
        return getHandleRect(handle).contains(new java.awt.Point(mouse.x, mouse.y));
    }



    private void applySelection() {
        if (selectedImage == null || selectionStart == null || selectionEnd == null) return;

        Rectangle rect = getNormalizedSelectionRect();


        Graphics g = canvas.getGraphics();
        g.drawImage(backupCanvas, 0, 0, null);


        g.setColor(Color.BLACK);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        g.dispose();


        for (int y = 0; y < selectedImage.getHeight(); y++) {
            for (int x = 0; x < selectedImage.getWidth(); x++) {
                if (selectionStart.x + x >= 0 && selectionStart.x + x < canvas.getWidth() &&
                        selectionStart.y + y >= 0 && selectionStart.y + y < canvas.getHeight()) {
                    int rgb = selectedImage.getRGB(x, y);
                    canvas.setRGB(selectionStart.x + x, selectionStart.y + y, rgb);
                }
            }
        }
    }



    public void clearCanvas() {

        int blackRGB = Color.BLACK.getRGB();
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = 0; x < canvas.getWidth(); x++) {
                canvas.setRGB(x, y, blackRGB);
            }
        }


        for (int y = 0; y < backupCanvas.getHeight(); y++) {
            for (int x = 0; x < backupCanvas.getWidth(); x++) {
                backupCanvas.setRGB(x, y, blackRGB);
            }
        }


        clearTemp();
    }

    private void clearTemp() {
        for (int y = 0; y < temp.getHeight(); y++) {
            for (int x = 0; x < temp.getWidth(); x++) {
                temp.setRGB(x, y, 0x00000000);
            }
        }
    }

    public void setCurrentColor(Color c) {
        this.currentColor = c;
    }

    public void setCurrentThickness(int thickness) {
        this.currentThickness = thickness;
    }

    public void setCurrentStyle(LineStyle style) {
        this.currentStyle = style;
    }

    public void setCurrentTool(ToolType tool) {
        this.currentTool = tool;
        if (tool != ToolType.SELECTION) {
            if (selectedImage != null && selectionStart != null && selectionEnd != null) {

                applySelection();
            }
            selectedImage = null;
            selectionStart = null;
            selectionEnd = null;
            isDraggingSelection = false;
        }
    }

    private void drawHandle(BufferedImage img, Point center, Color color) {
        int size = 6;
        int half = size / 2;
        int rgb = color.getRGB();

        for (int y = -half; y <= half; y++) {
            for (int x = -half; x <= half; x++) {
                int px = center.x + x;
                int py = center.y + y;
                if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                    img.setRGB(px, py, rgb);
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
        g.drawImage(temp, 0, 0, null);

        BufferedImage preview = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

        if(lastShapeType !=null&&lastShapeStart !=null&&lastShapeEnd !=null)

        {
            BufferedImage overlay = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

            switch (lastShapeType) {
                case LINE -> lineRasterizer.drawLine(overlay, lastShapeStart, lastShapeEnd, Color.RED, 1, LineStyle.DASHED);
                case RECTANGLE ->
                        rectangleRasterizer.drawRectangle(overlay, lastShapeStart, lastShapeEnd, Color.RED, 1, LineStyle.DASHED, false);
                case CIRCLE ->
                        circleRasterizer.drawCircle(overlay, lastShapeStart, lastShapeEnd, Color.RED, 1, LineStyle.DASHED);
            }

            drawHandle(overlay, lastShapeStart, Color.YELLOW);
            drawHandle(overlay, lastShapeEnd, Color.YELLOW);
            drawHandle(overlay, new Point((lastShapeStart.x + lastShapeEnd.x) / 2, (lastShapeStart.y + lastShapeEnd.y) / 2), Color.YELLOW);

            g.drawImage(overlay, 0, 0, null);
        }


        if (currentTool == ToolType.SELECTION && selectionStart != null && selectionEnd != null) {
            Rectangle rect = selectionRasterizer.getSelectionRect(selectionStart, selectionEnd);
            selectionRasterizer.drawSelectionBox(preview, selectionStart, selectionEnd);
            selectionRasterizer.drawHandles(preview, rect);


            if (selectedImage != null && !isDraggingSelection) {
                g.drawImage(selectedImage, selectionStart.x, selectionStart.y, null);
            } else if (selectedImage != null) {

                g.drawImage(selectedImage, selectionStart.x, selectionStart.y, null);
            }
        }

        if (currentTool == ToolType.POLYGON && !polygonRasterizer.isEmpty()) {
            List<Point> pts = polygonRasterizer.getPoints();
            for (int i = 1; i < pts.size(); i++) {
                lineRasterizer.drawLine(preview, pts.get(i - 1), pts.get(i), currentColor, currentThickness, currentStyle);
            }
            if (currentMouse != null) {
                lineRasterizer.drawLine(preview, pts.getLast(), currentMouse, Color.GRAY, currentThickness, currentStyle);
            }
        }

        if (start != null && currentMouse != null && currentTool != ToolType.POLYGON && currentTool != ToolType.SELECTION && selectedHandle == -1) {
            switch (currentTool) {
                case LINE -> lineRasterizer.drawLine(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
                case RECTANGLE -> rectangleRasterizer.drawRectangle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle, false);
                case CIRCLE -> circleRasterizer.drawCircle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
            }
        }

        g.drawImage(preview, 0, 0, null);
    }
}