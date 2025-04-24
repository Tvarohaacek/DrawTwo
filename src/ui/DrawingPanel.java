package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import model.LineStyle;
import model.Point;
import model.ToolType;
import rasterizer.CircleRasterizer;
import rasterizer.*;
import java.util.List;


/**
 * Panel pro kreslení. Umožňuje kreslit čáru mezi dvěma body.
 */
public class DrawingPanel extends JPanel {
    private BufferedImage canvas;
    private BufferedImage temp;  // Dočasná vrstva pro vykreslování během tahání
    private Point start;
    private Point currentMouse;
    private LineRasterizer lineRasterizer;
    private Color currentColor = Color.WHITE;
    private int currentThickness = 1;
    private LineStyle currentStyle = LineStyle.SOLID;
    private ToolType currentTool = ToolType.LINE;
    private RectangleRasterizer rectangleRasterizer;
    private CircleRasterizer circleRasterizer;
    private PolygonRasterizer polygonRasterizer;
    private FillRasterizer fillRasterizer;

    private SelectionRasterizer selectionRasterizer;
    private boolean isSelecting = false;
    private Point selectionStart, selectionEnd;
    private BufferedImage selectedImage;
    private boolean draggingSelection = false;
    private Point dragOffset;



    public DrawingPanel(int width, int height) {
        this.setPreferredSize(new Dimension(width, height));
        lineRasterizer = new LineRasterizer();
        rectangleRasterizer = new RectangleRasterizer(lineRasterizer);
        circleRasterizer = new CircleRasterizer();
        polygonRasterizer = new PolygonRasterizer(lineRasterizer);
        fillRasterizer = new FillRasterizer();
        selectionRasterizer = new SelectionRasterizer(lineRasterizer);
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        clearCanvas();

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point clicked = new Point(e.getX(), e.getY());

                if (currentTool == ToolType.POLYGON) {
                    if (!polygonRasterizer.isEmpty() && polygonRasterizer.isCloseToFirst(clicked, 10)) {
                        // Zavři polygon
                        polygonRasterizer.drawPolygon(canvas, currentColor, currentThickness, currentStyle, true);
                        polygonRasterizer.clear();
                    } else {
                        polygonRasterizer.addPoint(clicked);
                    }
                } else {
                    start = clicked;
                    currentMouse = null;

                    // Přepnutí nástroje uzavře rozdělaný polygon
                    if (!polygonRasterizer.isEmpty()) {
                        polygonRasterizer.drawPolygon(canvas, currentColor, currentThickness, currentStyle, true);
                        polygonRasterizer.clear();
                    }
                }

                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                currentMouse = new Point(e.getX(), e.getY());
                boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                // Finální vykreslení do hlavního plátna
                switch (currentTool) {
                    case LINE -> lineRasterizer.drawLine(canvas, start, currentMouse, currentColor, currentThickness, currentStyle);
                    case RECTANGLE -> rectangleRasterizer.drawRectangle(canvas, start, currentMouse, currentColor, currentThickness, currentStyle, shift);
                    case CIRCLE -> circleRasterizer.drawCircle(canvas, start, currentMouse, currentColor, currentThickness, currentStyle);
                    case FILL -> fillRasterizer.floodFill(canvas, new Point(e.getX(), e.getY()), currentColor);

                }

                // Vyčisti dočasnou vrstvu
                for (int y = 0; y < temp.getHeight(); y++) {
                    for (int x = 0; x < temp.getWidth(); x++) {
                        temp.setRGB(x, y, new Color(0, 0, 0, 0).getRGB()); // průhledné
                    }
                }

                repaint();
                start = null;
                currentMouse = null;

            }
            @Override
            public void mouseDragged(MouseEvent e) {
                currentMouse = new Point (e.getX(), e.getY());
                boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;

                // Vyčisti dočasnou vrstvu
                for (int y = 0; y < temp.getHeight(); y++) {
                    for (int x = 0; x < temp.getWidth(); x++) {
                        temp.setRGB(x, y, new Color(0, 0, 0, 0).getRGB()); // plně průhledné
                    }
                }

                // Vykresli tvar podle aktuálního nástroje
                switch (currentTool) {
                    case LINE -> lineRasterizer.drawLine(temp, start, currentMouse, currentColor, currentThickness, currentStyle);
                    case RECTANGLE -> {
                        // Výpočet souřadnic pro obdélník (nebo čtverec při držení shift)
                        int x1 = start.x;
                        int y1 = start.y;
                        int x2 = currentMouse.x;
                        int y2 = currentMouse.y;

                        // Pokud je shift, vykreslíme čtverec
                        if (shift) {
                            int size = Math.min(Math.abs(x2 - x1), Math.abs(y2 - y1));
                            x2 = x1 + (x2 < x1 ? -size : size);
                            y2 = y1 + (y2 < y1 ? -size : size);
                        }

                        // Vykreslíme celý obdélník nebo čtverec dočasně na dočasné plátno
                        rectangleRasterizer.drawRectangle(temp, new Point(x1, y1), new Point(x2, y2), currentColor, currentThickness, currentStyle, shift);
                    }
                    case CIRCLE -> circleRasterizer.drawCircle(temp, start, currentMouse, currentColor, currentThickness, currentStyle);
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

    private void clearCanvas() {
        Graphics g = canvas.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.dispose();
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
            selectedImage = null;
            selectionStart = null;
            selectionEnd = null;
            isSelecting = false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Vykresli trvalou a dočasnou vrstvu
        g.drawImage(canvas, 0, 0, null);
        g.drawImage(temp, 0, 0, null);

        // Vytvoříme čistou náhledovou vrstvu
        BufferedImage preview = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Výběr
        if (currentTool == ToolType.SELECTION && selectionStart != null && selectionEnd != null) {
            selectionRasterizer.drawSelectionBox(preview, selectionStart, selectionEnd);
            if (selectedImage != null) {
                g.drawImage(selectedImage, selectionStart.x, selectionStart.y, null);
            }
        }

        // Polygon
        if (currentTool == ToolType.POLYGON && !polygonRasterizer.isEmpty()) {
            List<Point> pts = polygonRasterizer.getPoints();
            for (int i = 1; i < pts.size(); i++) {
                lineRasterizer.drawLine(preview, pts.get(i - 1), pts.get(i), currentColor, currentThickness, currentStyle);
            }
            if (currentMouse != null) {
                lineRasterizer.drawLine(preview, pts.get(pts.size() - 1), currentMouse, Color.GRAY, currentThickness, currentStyle);
            }
        }

        // Ostatní nástroje (čára, obdélník, kruh)
        if (start != null && currentMouse != null &&
                currentTool != ToolType.POLYGON && currentTool != ToolType.SELECTION) {

            switch (currentTool) {
                case LINE -> lineRasterizer.drawLine(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
                case RECTANGLE -> {
                    boolean shift = false;
                    rectangleRasterizer.drawRectangle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle, shift);
                }
                case CIRCLE -> {
                    int dx = currentMouse.x - start.x;
                    int dy = currentMouse.y - start.y;
                    int radius = (int) Math.sqrt(dx * dx + dy * dy);
                    circleRasterizer.drawCircle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
                }
            }
        }

        // Nakonec vykreslíme náhled
        g.drawImage(preview, 0, 0, null);
    }


}
