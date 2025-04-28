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
    // Přidáme další buffery pro práci s výběrem
    private BufferedImage backupCanvas; // Pro ukládání stavu před výběrem
    private BufferedImage originalCanvas; // Pro ukládání stavu před manipulací s čárou
    private Point start;
    private Point currentMouse;
    private LineRasterizer lineRasterizer;
    private RectangleRasterizer rectangleRasterizer;
    private CircleRasterizer circleRasterizer;
    private PolygonRasterizer polygonRasterizer;
    private FillRasterizer fillRasterizer;
    private SelectionRasterizer selectionRasterizer;

    private Color currentColor = Color.WHITE;
    private int currentThickness = 1;
    private LineStyle currentStyle = LineStyle.SOLID;
    private ToolType currentTool = ToolType.LINE;

    private Point selectionStart, selectionEnd;
    private BufferedImage selectedImage;
    private boolean isDraggingSelection = false;
    private int selectionHandleIndex = -1;
    private Point dragOffset;

    // Proměnné pro manipulaci s čárou
    private Point currentLineStart, currentLineEnd;
    private boolean isDraggingLinePoint = false;
    private int selectedLinePointIndex = -1; // -1: none, 0: start point, 1: end point
    private static final int LINE_HANDLE_RADIUS = 5; // Velikost úchytu koncového bodu
    private boolean isShiftDown = false; // Sleduje stav Shift klávesy
    private boolean lineBeingManipulated = false; // Identifikuje, zda právě manipulujeme s čárou

    public DrawingPanel(int width, int height) {
        this.setPreferredSize(new Dimension(width, height));
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        backupCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        originalCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

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
                isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                // Kontrola, zda jsme klikli na koncový bod aktuální čáry
                if (currentLineStart != null && currentLineEnd != null && currentTool == ToolType.LINE) {
                    if (isNearPoint(p, currentLineStart, LINE_HANDLE_RADIUS)) {
                        // Uložíme stav plátna před manipulací
                        saveCanvasState();
                        selectedLinePointIndex = 0;
                        isDraggingLinePoint = true;
                        lineBeingManipulated = true;
                        currentMouse = p;
                        return;
                    }
                    if (isNearPoint(p, currentLineEnd, LINE_HANDLE_RADIUS)) {
                        // Uložíme stav plátna před manipulací
                        saveCanvasState();
                        selectedLinePointIndex = 1;
                        isDraggingLinePoint = true;
                        lineBeingManipulated = true;
                        currentMouse = p;
                        return;
                    }
                }

                // Pokud jsme neklikli na koncový bod, nastavíme start pro novou čáru
                currentMouse = p;
                start = p;
                lineBeingManipulated = false;

                if (currentTool == ToolType.SELECTION) {
                    Rectangle rect = selectionStart != null && selectionEnd != null
                            ? selectionRasterizer.getSelectionRect(selectionStart, selectionEnd)
                            : null;

                    if (rect != null) {
                        selectionHandleIndex = selectionRasterizer.getHandleIndex(p, rect);
                        if (selectionHandleIndex != -1) return;

                        if (rect.contains(new java.awt.Point(p.x, p.y))) {
                            isDraggingSelection = true;
                            dragOffset = new Point(p.x - rect.x, p.y - rect.y);
                            return;
                        }
                    }

                    // Pokud začínáme nový výběr, zrušíme starý a uložíme kopii plátna
                    backupCurrentCanvas();
                    selectionStart = p;
                    selectionEnd = p;
                    selectedImage = null;
                } else if (currentTool != ToolType.LINE) {
                    // Pokud přepneme na jiný nástroj, zrušíme možnost manipulace s čárou
                    currentLineStart = null;
                    currentLineEnd = null;
                }

                if (currentTool == ToolType.POLYGON) {
                    if (!polygonRasterizer.isEmpty() && polygonRasterizer.isCloseToFirst(p, 10)) {
                        polygonRasterizer.drawPolygon(canvas, currentColor, currentThickness, currentStyle, true);
                        polygonRasterizer.clear();
                    } else {
                        polygonRasterizer.addPoint(p);
                    }
                }

                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point end = new Point(e.getX(), e.getY());
                isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                if (isDraggingLinePoint && currentLineStart != null && currentLineEnd != null) {
                    // Aktualizace pouze bodu, který jsme táhli
                    if (selectedLinePointIndex == 0) {
                        if (isShiftDown) {
                            currentLineStart = lineRasterizer.snapTo45Degrees(currentLineEnd, end);
                        } else {
                            currentLineStart = end;
                        }
                    } else {
                        if (isShiftDown) {
                            currentLineEnd = lineRasterizer.snapTo45Degrees(currentLineStart, end);
                        } else {
                            currentLineEnd = end;
                        }
                    }

                    // Obnovíme plátno bez aktuální čáry
                    restoreCanvasState();

                    // Vykreslíme aktualizovanou čáru
                    lineRasterizer.drawLine(canvas, currentLineStart, currentLineEnd,
                            currentColor, currentThickness, currentStyle);

                    isDraggingLinePoint = false;
                    selectedLinePointIndex = -1;
                    lineBeingManipulated = false;
                    clearTemp();
                    repaint();
                    return;
                }

                if (isDraggingSelection) {
                    // Když ukončíme tažení výběru, provedeme skutečné přesunutí na plátně
                    applySelection();
                    isDraggingSelection = false;
                    repaint();
                    return;
                }

                if (selectionHandleIndex != -1) {
                    // Když ukončíme změnu velikosti, aplikujeme změny na plátno
                    Rectangle rect = selectionRasterizer.resizeSelection(selectionStart, selectionEnd, selectionHandleIndex, end);
                    selectionStart = new Point(rect.x, rect.y);
                    selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
                    selectionHandleIndex = -1;

                    // Znovu získáme správný obrázek po změně velikosti
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
                    case LINE:
                        if (!lineBeingManipulated && start != null) {
                            Point endPoint = isShiftDown ? lineRasterizer.snapTo45Degrees(start, end) : end;
                            lineRasterizer.drawLine(canvas, start, endPoint, currentColor, currentThickness, currentStyle);
                            saveCurrentLine(start, endPoint);  // Uložíme čáru pro budoucí manipulaci
                        }
                        break;
                    case RECTANGLE:
                        rectangleRasterizer.drawRectangle(canvas, start, end, currentColor, currentThickness, currentStyle, isShiftDown);
                        break;
                    case CIRCLE:
                        circleRasterizer.drawCircle(canvas, start, end, currentColor, currentThickness, currentStyle);
                        break;
                    case FILL:
                        fillRasterizer.floodFill(canvas, end, currentColor);
                        break;
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
                isShiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                clearTemp();

                if (isDraggingLinePoint && currentLineStart != null && currentLineEnd != null) {
                    // Kopie bodů pro náhled
                    Point previewStart = new Point(currentLineStart.x, currentLineStart.y);
                    Point previewEnd = new Point(currentLineEnd.x, currentLineEnd.y);

                    // Aktualizace pouze bodu, který táhneme
                    if (selectedLinePointIndex == 0) {
                        if (isShiftDown) {
                            previewStart = lineRasterizer.snapTo45Degrees(currentLineEnd, dragged);
                        } else {
                            previewStart = dragged;
                        }
                    } else {
                        if (isShiftDown) {
                            previewEnd = lineRasterizer.snapTo45Degrees(currentLineStart, dragged);
                        } else {
                            previewEnd = dragged;
                        }
                    }

                    // Vykreslení náhledu - nejprve obnovíme původní plátno
                    Graphics g = temp.getGraphics();
                    g.drawImage(originalCanvas, 0, 0, null);
                    g.dispose();

                    // Poté vykreslíme aktualizovanou čáru
                    lineRasterizer.drawLine(temp, previewStart, previewEnd,
                            currentColor, currentThickness, currentStyle);
                    repaint();
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

                if (currentTool == ToolType.SELECTION) {
                    selectionEnd = dragged;
                    repaint();
                    return;
                }

                switch (currentTool) {
                    case LINE:
                        if (!lineBeingManipulated && start != null) {
                            Point endPoint = isShiftDown ? lineRasterizer.snapTo45Degrees(start, dragged) : dragged;
                            lineRasterizer.drawLine(temp, start, endPoint, currentColor, currentThickness, currentStyle);
                        }
                        break;
                    case RECTANGLE:
                        rectangleRasterizer.drawRectangle(temp, start, dragged, currentColor, currentThickness, currentStyle, isShiftDown);
                        break;
                    case CIRCLE:
                        circleRasterizer.drawCircle(temp, start, dragged, currentColor, currentThickness, currentStyle);
                        break;
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

    // Metoda pro uložení aktuálního stavu plátna před manipulací s čárou
    private void saveCanvasState() {
        Graphics g = originalCanvas.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
    }

    // Metoda pro obnovení stavu plátna
    private void restoreCanvasState() {
        Graphics g = canvas.getGraphics();
        g.drawImage(originalCanvas, 0, 0, null);
        g.dispose();
    }

    // Metoda pro kontrolu, zda je bod blízko jinému bodu
    private boolean isNearPoint(Point p1, Point p2, int radius) {
        int dx = p1.x - p2.x;
        int dy = p1.y - p2.y;
        return (dx * dx + dy * dy) <= radius * radius;
    }

    // Metoda pro uložení aktuální čáry pro manipulaci
    private void saveCurrentLine(Point start, Point end) {
        currentLineStart = start;
        currentLineEnd = end;
    }

    // Nová metoda pro zálohování aktuálního stavu plátna
    private void backupCurrentCanvas() {
        // Kopírujeme celý canvas pro pozdější obnovení
        Graphics g = backupCanvas.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
    }

    // Nová metoda pro aktualizaci vybraného obrázku
    private void updateSelectedImage() {
        if (selectionStart == null || selectionEnd == null) return;

        // Získáme správný obdélník výběru
        Rectangle rect = getNormalizedSelectionRect();

        // Kontrola platnosti výběru
        if (rect.width <= 0 || rect.height <= 0) {
            selectedImage = null;
            return;
        }

        // Kontrola hranic plátna
        rect.x = Math.max(0, rect.x);
        rect.y = Math.max(0, rect.y);
        rect.width = Math.min(canvas.getWidth() - rect.x, rect.width);
        rect.height = Math.min(canvas.getHeight() - rect.y, rect.height);

        if (rect.width <= 0 || rect.height <= 0) {
            selectedImage = null;
            return;
        }

        // Vytvoříme nový obrázek pro výběr
        selectedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);

        // Kopírujeme pixely z plátna do výběru
        for (int y = 0; y < rect.height; y++) {
            for (int x = 0; x < rect.width; x++) {
                int rgb = canvas.getRGB(rect.x + x, rect.y + y);
                selectedImage.setRGB(x, y, rgb);
            }
        }

        // Aktualizujeme souřadnice výběru
        selectionStart = new Point(rect.x, rect.y);
        selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
    }

    // Metoda pro normalizaci obdélníku výběru (aby šířka a výška nebyly záporné)
    private Rectangle getNormalizedSelectionRect() {
        int x = Math.min(selectionStart.x, selectionEnd.x);
        int y = Math.min(selectionStart.y, selectionEnd.y);
        int width = Math.abs(selectionEnd.x - selectionStart.x);
        int height = Math.abs(selectionEnd.y - selectionStart.y);

        return new Rectangle(x, y, width, height);
    }

    // Nová metoda pro aplikování výběru na plátno
    private void applySelection() {
        if (selectedImage == null || selectionStart == null || selectionEnd == null) return;

        Rectangle rect = getNormalizedSelectionRect();

        // Obnovíme původní plátno z zálohy
        Graphics g = canvas.getGraphics();
        g.drawImage(backupCanvas, 0, 0, null);

        // Vymažeme oblast pod výběrem (černou barvou)
        g.setColor(Color.BLACK);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        g.dispose();

        // Nakreslíme vybraný obrázek na nové pozici
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

    private void clearCanvas() {
        Graphics g = canvas.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.dispose();

        // Také vyčistíme záložní plátno
        g = backupCanvas.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, backupCanvas.getWidth(), backupCanvas.getHeight());
        g.dispose();

        // A také originalCanvas
        g = originalCanvas.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, originalCanvas.getWidth(), originalCanvas.getHeight());
        g.dispose();
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
                // Aplikujeme výběr na plátno před změnou nástroje
                applySelection();
            }
            selectedImage = null;
            selectionStart = null;
            selectionEnd = null;
            isDraggingSelection = false;
        }

        // Pokud přepneme na jiný nástroj než čára, zrušíme možnost manipulace s čárou
        if (tool != ToolType.LINE) {
            currentLineStart = null;
            currentLineEnd = null;
            isDraggingLinePoint = false;
            lineBeingManipulated = false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
        g.drawImage(temp, 0, 0, null);

        BufferedImage preview = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = preview.createGraphics();

        if (currentTool == ToolType.SELECTION && selectionStart != null && selectionEnd != null) {
            Rectangle rect = selectionRasterizer.getSelectionRect(selectionStart, selectionEnd);
            selectionRasterizer.drawSelectionBox(preview, selectionStart, selectionEnd);
            selectionRasterizer.drawHandles(preview, rect);

            // Vykreslíme vybraný obrázek
            if (selectedImage != null && !isDraggingSelection) {
                g.drawImage(selectedImage, selectionStart.x, selectionStart.y, null);
            } else if (selectedImage != null) {
                // Při tažení kreslíme jen ohraničení (originál zůstává na místě)
                g.drawImage(selectedImage, selectionStart.x, selectionStart.y, null);
            }
        }

        if (currentTool == ToolType.POLYGON && !polygonRasterizer.isEmpty()) {
            List<Point> pts = polygonRasterizer.getPoints();
            for (int i = 1; i < pts.size(); i++) {
                lineRasterizer.drawLine(preview, pts.get(i - 1), pts.get(i), currentColor, currentThickness, currentStyle);
            }
            if (currentMouse != null) {
                lineRasterizer.drawLine(preview, pts.get(pts.size() - 1), currentMouse, Color.GRAY, currentThickness, currentStyle);
            }
        }

        // Vykreslení koncových bodů čáry, pokud máme nějakou vybranou
        if (currentLineStart != null && currentLineEnd != null && currentTool == ToolType.LINE) {
            g2d.setColor(Color.RED);
            g2d.fillOval(currentLineStart.x - LINE_HANDLE_RADIUS,
                    currentLineStart.y - LINE_HANDLE_RADIUS,
                    LINE_HANDLE_RADIUS * 2, LINE_HANDLE_RADIUS * 2);
            g2d.fillOval(currentLineEnd.x - LINE_HANDLE_RADIUS,
                    currentLineEnd.y - LINE_HANDLE_RADIUS,
                    LINE_HANDLE_RADIUS * 2, LINE_HANDLE_RADIUS * 2);
        }

        // Náhled při kreslení objektů - pouze pokud nejsme v režimu manipulace s čárou
        if (start != null && currentMouse != null && !lineBeingManipulated) {
            g2d.setColor(Color.GRAY);
            switch (currentTool) {
                case LINE:
                    Point endPoint = isShiftDown ?
                            lineRasterizer.snapTo45Degrees(start, currentMouse) : currentMouse;
                    lineRasterizer.drawLine(preview, start, endPoint, Color.GRAY, currentThickness, currentStyle);
                    break;
                case RECTANGLE:
                    rectangleRasterizer.drawRectangle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle, isShiftDown);
                    break;
                case CIRCLE:
                    circleRasterizer.drawCircle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
                    break;
            }
        }

        g2d.dispose();
        g.drawImage(preview, 0, 0, null);
    }
}