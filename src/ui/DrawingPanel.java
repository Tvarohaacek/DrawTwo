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
    private boolean shift = false;



    private Point selectionStart, selectionEnd;
    private BufferedImage selectedImage;
    BufferedImage overlay;
    private boolean isDraggingSelection = false;
    private int selectionHandleIndex = -1;
    private Point dragOffset;

    /*Primární a veškeré logické záležitosti akcí uživatele,
    * celkové chování aplikace a logika
    * "proč to funguje zrovna takhle"*/

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

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "clearCanvas");
        getActionMap().put("clearCanvas", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearWholeCanvas();
            }
        });

        clearCanvas();

        MouseAdapter mouseHandler = new MouseAdapter() {

            /**
             * mousePressed zaznamená počáteční bod stisku myší (start)
             * a i aktuální pozici (currentMouse)*/
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = new Point(e.getX(), e.getY());
                currentMouse = p;
                start = p;

                /*Pro nástroje štětce a a gumy zaznamená poslední pozici pro plynulé kreslení
                * používá se v drawInterpolated... metodách */
                if (currentTool == ToolType.BRUSH || currentTool == ToolType.ERASER) {
                    lastMousePoint = p;
                }

                /*kontroluje editaci posledního tvaru*/
                if (lastShapeType != null) {
                    Point handle1 = lastShapeStart;
                    Point handle2 = lastShapeEnd;
                    Point center = new Point((handle1.x + handle2.x) / 2, (handle1.y + handle2.y) / 2);


                    //podmínky níže určují, na který "manipulovatelný bod" se kliklo
                    //a podle toho se i zachovají
                    //krajní body zvětšují, prostřední hýbe
                    if (isOnHandle(p, handle1)) {
                        selectedHandle = 0;
                        isEditingLastShape = true;
                        eraseLastShapeFromCanvas();
                        clearTemp();
                        return;
                    } else if (isOnHandle(p, handle2)) {
                        selectedHandle = 1;
                        isEditingLastShape = true;
                        eraseLastShapeFromCanvas();
                        clearTemp();
                        return;
                    } else if (isOnHandle(p, center)) {
                        selectedHandle = 2;
                        dragOffset = new Point(p.x - center.x, p.y - center.y);
                        isEditingLastShape = true;
                        eraseLastShapeFromCanvas();
                        clearTemp();
                        return;
                    }
                }

                //switch, určuje co se děje podle aktuálního nástroje
                switch (currentTool) {

                    /*pokud polygon není prázdný
                    * kliknutí blízko polygon uzavře
                    * jinak přidá nový bod do polygonu*/
                    case POLYGON -> {if (!polygonRasterizer.isEmpty() && polygonRasterizer.isCloseToFirst(p, 10)) {
                        polygonRasterizer.drawPolygon(canvas, currentColor, currentThickness, currentStyle, true);
                        polygonRasterizer.clear();
                    } else {
                        polygonRasterizer.addPoint(p);
                    }}
                    /*Zkontroluje, jestli se chytil výběr pro přesun
                    * nebo pro změnu velikosti
                    *
                    * Pokud nebyl existující výběr nebo kliknutí nebylo uvnitř něj,
                    * uloží aktuální plátno do zálohy backupCurrentCanvas() a
                    * nastaví počáteční bod výběru selectionStart.*/
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
                    /*Štětec i Guma okamžitě kreslí bod na stisknuté pozici*/
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
                //volá repaint, ta automaticky volá i PaintComponent
                //je to základní metoda image observeru
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point end = new Point(e.getX(), e.getY());
                //zaznamená koncový bod, kde se pustila myš
                shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
                //kontroluje zmáčknutí shiftu

                //pro štětec a gumu resetuje lastMousePoint.
                if (currentTool == ToolType.BRUSH || currentTool == ToolType.ERASER) {
                    lastMousePoint = null;
                }

                //Pokud probíhá výběr, aplikuje ho na plátno a resetuje logiku, jestli se přetahuje nebo ne
                if (isDraggingSelection) {

                    applySelection();
                    isDraggingSelection = false;
                    repaint();
                    return;
                }

                //Pokud se upravoval tvar, vymažne dočasné plátno
                //a nakreslí ho napevno na spodní plátno
                if (isEditingLastShape) {
                    clearTemp();
                    drawLastShapeToCanvas();
                    isEditingLastShape = false;
                    selectedHandle = -1;
                    repaint();
                    return;
                }

                /*Pokud se měnila velikost výběru,
                * přepočítá ho a resetuje index úchytu pro logiku v selectionRasterizeru*/
                if (selectionHandleIndex != -1) {

                    Rectangle rect = selectionRasterizer.resizeSelection(selectionStart, selectionEnd, selectionHandleIndex, end);
                    selectionStart = new Point(rect.x, rect.y);
                    selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
                    selectionHandleIndex = -1;


                    updateSelectedImage();
                    repaint();
                    return;
                }


                /*
                * Pro  čáru, obdélník a kruh dokončí kreslení tvaru
                * na hlavní plátno pomocí příslušného rasterizeru.
                * Zaznamená typ, počáteční a koncový bod posledního
                * nakresleného tvaru pro pozdější editaci.
                * Pokud probíhala editace, kreslí se na dočasné plátno.*/
                switch (currentTool) {
                    case LINE -> {
                        end = shift ? lineRasterizer.snapTo45Degrees(start, end) : end;
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
                            drawLastShapeToTemp();
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
                            drawLastShapeToTemp();
                            repaint();
                            return;
                        }

                    }
                    //Pokud je aktivní výběr, nastaví koncový bod
                    // výběru selectionEnd a aktualizuje vybranou část rastru
                    case SELECTION -> {
                        selectionEnd = end;
                        updateSelectedImage();
                        repaint();
                        return;
                    }
                    //při uvolnění myši se spustí floodFill algoritmus
                    case FILL -> fillRasterizer.floodFill(canvas, end, currentColor);


                }


                //resetuje důležité proměnné
                clearTemp();
                start = null;
                currentMouse = null;
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                //zapamatuje si aktuální pozici myši během tažení
                Point dragged = new Point(e.getX(), e.getY());
                currentMouse = dragged;

                shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;


                clearTemp();
                /* Pokud probíhá editace, aktualizuje start nebo end point
                 (nebo posouvá celý tvar) podle vybraného bodu chycení
                  a překreslí tvar na temp plátno.*/
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

                //pokud probíhá přetahování, přepočítá novou pozici výběru.
                if (isDraggingSelection && dragOffset != null) {
                    Point newTopLeft = new Point(dragged.x - dragOffset.x, dragged.y - dragOffset.y);
                    selectionStart = newTopLeft;
                    selectionEnd = new Point(newTopLeft.x + selectedImage.getWidth(), newTopLeft.y + selectedImage.getHeight());
                    repaint();
                    return;
                }
                //pokud se mění velikost, taky přepočítá pozici výběru
                if (selectionHandleIndex != -1 && selectionStart != null && selectionEnd != null) {
                    Rectangle rect = selectionRasterizer.resizeSelection(selectionStart, selectionEnd, selectionHandleIndex, dragged);
                    selectionStart = new Point(rect.x, rect.y);
                    selectionEnd = new Point(rect.x + rect.width, rect.y + rect.height);
                    repaint();
                    return;
                }

                /*pro čáru, obdélník a kruh kreslí náhled tvaru na temp plátno.
                * Pro výběr aktualizuje koncový bod výběru.
                * Pro štětec a gumu kreslí čáru pro plný tah pomocí Interpolated metody níže*/
                switch (currentTool) {
                    case LINE -> {
                        dragged = shift ? lineRasterizer.snapTo45Degrees(start, dragged) : dragged;
                        lineRasterizer.drawLine(temp, start, dragged, currentColor, currentThickness, currentStyle);
                    }
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
                //Zaznamenává aktuální pozici myši
                currentMouse = new Point(e.getX(), e.getY());
                //repaint pro aktualizaci zobrazení - třeba náhled polygonu
                repaint();
            }
        };

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
    }

    //Pomocná metoda pro štětec. Vykreslí normální "netrhanou" čáru mezi dvěma
    // body interpolací menších bodů, aby byl tah plynulý i při rychlém pohybu myši.
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

    /*to stejné jako pro štětec, ale je to guma*/
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

    /*ymaže oblast kde se nacházel posledí nakreslený tvar
    * lepší způsob jsem na to nenašel*/
    private void eraseLastShapeFromCanvas() {
        if (lastShapeType == null || lastShapeStart == null || lastShapeEnd == null) return;


        Rectangle bounds = getShapeBoundingBox(lastShapeStart, lastShapeEnd, lastShapeType);

        for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
            for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
                if (x >= 0 && x < canvas.getWidth() && y >= 0 && y < canvas.getHeight()) {
                    canvas.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
    }

    /*pomocná metoda pro eraseLastShapeFromCanvas().
     * vrací oobdélník přesouvaného tvaru definovaný
     * dvěma body. Pro čáru přidává malý okraj kvůli tloušťce.
     * Funguje jen na čístě černém plátně*/
    private Rectangle getShapeBoundingBox(Point p1, Point p2, ToolType type) {
        int x = Math.min(p1.x, p2.x);
        int y = Math.min(p1.y, p2.y);
        int width = Math.abs(p2.x - p1.x);
        int height = Math.abs(p2.y - p1.y);


        if (type == ToolType.LINE) {
            return new Rectangle(x - 2, y - 2, width + 4, height + 4);
        }

        return new Rectangle(x, y, width, height);
    }

    /*
    * Další pomocná metoda pro editaci posledního tvaru
    * nakreslí ho na dočasné plátno*/
    private void drawLastShapeToTemp() {
        if (lastShapeType == null || lastShapeStart == null || lastShapeEnd == null) return;

        switch (lastShapeType) {
            case LINE -> lineRasterizer.drawLine(temp, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
            case RECTANGLE -> rectangleRasterizer.drawRectangle(temp, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle, false);
            case CIRCLE -> circleRasterizer.drawCircle(temp, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
        }
    }
    /*
    * poslední tvar dá na hlavní canvas - pak je neupravitelný*/
    private void drawLastShapeToCanvas() {
        if (lastShapeType == null || lastShapeStart == null || lastShapeEnd == null) return;

        switch (lastShapeType) {
            case LINE -> lineRasterizer.drawLine(canvas, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
            case RECTANGLE -> rectangleRasterizer.drawRectangle(canvas, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle, false);
            case CIRCLE -> circleRasterizer.drawCircle(canvas, lastShapeStart, lastShapeEnd, currentColor, currentThickness, currentStyle);
        }
    }

    /*Vytvoří backup pro výběr, aby se logivky výběr mohl zrušit nebo vracet*/
    private void backupCurrentCanvas() {

        Graphics g = backupCanvas.getGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
    }

    /*z hlavního plátna uloží obsah oblasti
    * a uložího do SelectedImage
    * metoda se volá po dokončení výběru
    * ošetřuje případy hranice plátna*/
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

    /*Vrací rectangle objekt se souřadnicemi výběru*/
    private Rectangle getNormalizedSelectionRect() {
        int x = Math.min(selectionStart.x, selectionEnd.x);
        int y = Math.min(selectionStart.y, selectionEnd.y);
        int width = Math.abs(selectionEnd.x - selectionStart.x);
        int height = Math.abs(selectionEnd.y - selectionStart.y);

        return new Rectangle(x, y, width, height);
    }

    /*Pomocná metoda pro detekci a kreslení úchytů eidtace tvarů*/
    private Rectangle getHandleRect(Point p) {
        int size = 6;
        return new Rectangle(p.x - size / 2, p.y - size / 2, size, size);
    }

    /*
    * detekce, jestli je myš na úchytu*/
    private boolean isOnHandle(Point mouse, Point handle) {
        return getHandleRect(handle).contains(new java.awt.Point(mouse.x, mouse.y));
    }


    /*Překreslí výběr na hlavní plátno,
    * předtím se obnoví stav plátna ze zálohy backupCanvas
    **/
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

    /*kreslí "body držení" pro manipulaci s měnitelnými tvary*/
    private void drawHandle(BufferedImage img, Point center) {
        int size = 6;
        int half = size / 2;
        int rgb = Color.YELLOW.getRGB();

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


/*čistí canvas*/
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

    /*clear metoda pro reset plátna*/
    private void clearWholeCanvas() {
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = 0; x < canvas.getWidth(); x++) {
                canvas.setRGB(x, y, 0x00000000);
            }
        }
        clearTemp();
        clearOverlayCanvas();
        clearCanvas();

        lastShapeType = null;
        lastShapeStart = null;
        lastShapeEnd = null;

        repaint();
    }

    private void clearOverlayCanvas() {
        if (overlay != null)
        {
            for (int y = 0; y < overlay.getHeight(); y++) {
                for (int x = 0; x < overlay.getWidth(); x++) {}
                overlay.setRGB(overlay.getWidth() - 1, y, 0x00000000);
            }
        }

    }

    /*kompletně vyčístí backup na průhledno*/
    private void clearTemp() {
        for (int y = 0; y < temp.getHeight(); y++) {
            for (int x = 0; x < temp.getWidth(); x++) {
                temp.setRGB(x, y, 0x00000000);
            }
        }
    }


    //jednoduché settery a gettery
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

        lastShapeType = null;
        lastShapeStart = null;
        lastShapeEnd = null;

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



    /*Overriduje metodu paintComponent, která se volá při každém repaint
    * Překlesí komponentu
    * Vykreslí hlavní plátno
    * Vykreslí dočasné plátno
    * */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, null);
        g.drawImage(temp, 0, 0, null);

        BufferedImage preview = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

        if(lastShapeType !=null&&lastShapeStart !=null&&lastShapeEnd !=null)

        {
            overlay = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);

            //zobrazí overlay úchyty pro editaci - pokud existuje poslední nakreslený tvar
            //vykreslí červeně orámovaný úchyt
            switch (lastShapeType) {
                case LINE -> lineRasterizer.drawLine(overlay, lastShapeStart, lastShapeEnd, Color.RED, 1, LineStyle.DASHED);

                case RECTANGLE ->
                        rectangleRasterizer.drawRectangle(overlay, lastShapeStart, lastShapeEnd, Color.RED, 1, LineStyle.DASHED, false);
                case CIRCLE ->
                        circleRasterizer.drawCircle(overlay, lastShapeStart, lastShapeEnd, Color.RED, 1, LineStyle.DASHED);
            }

            drawHandle(overlay, lastShapeStart);
            drawHandle(overlay, lastShapeEnd);
            drawHandle(overlay, new Point((lastShapeStart.x + lastShapeEnd.x) / 2, (lastShapeStart.y + lastShapeEnd.y) / 2));

            g.drawImage(overlay, 0, 0, null);
        }

        /*
        * Pokud je aktivní nástroj výběr a je definován
        * vykresli ohraničovací rámeček a úchyty
        *   Pokud vybraná část existuje a neprobíhá přetahování
        *   vykreslí se na začáteční pozici výběru
        *
        *   během přetahování se vykresluje i selectedImage*/
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

        /*náhled polygonu
        * pokud je aktivní nástroj Polygon a existují již nějaké body
        *   vykreslí čáry mezi nimi a dočasnou čáru od
        *   posledního bodu k aktuální pozici myši.*/
        if (currentTool == ToolType.POLYGON && !polygonRasterizer.isEmpty()) {
            List<Point> pts = polygonRasterizer.getPoints();
            for (int i = 1; i < pts.size(); i++) {
                lineRasterizer.drawLine(preview, pts.get(i - 1), pts.get(i), currentColor, currentThickness, currentStyle);
            }
            if (currentMouse != null) {
                lineRasterizer.drawLine(preview, pts.getLast(), currentMouse, Color.GRAY, currentThickness, currentStyle);
            }
        }
        /*
        * Pokud se kreslí čára, obdélník nebo kruh (a neděje se nic jiného),
        * vykreslí se šedý náhled tvaru na základě počátečního bodu
        * (start) a aktuální pozice myši (currentMouse).*/
        if (start != null && currentMouse != null && currentTool != ToolType.POLYGON && currentTool != ToolType.SELECTION && selectedHandle == -1) {
            switch (currentTool) {
                case LINE -> {
                    currentMouse = shift ? lineRasterizer.snapTo45Degrees(start, currentMouse) : currentMouse;
                    lineRasterizer.drawLine(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
                }
                case RECTANGLE -> rectangleRasterizer.drawRectangle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle, false);
                case CIRCLE -> circleRasterizer.drawCircle(preview, start, currentMouse, Color.GRAY, currentThickness, currentStyle);
            }
        }

        g.drawImage(preview, 0, 0, null);
        //vykresli pomocné plátno pro náhledy a výběr

    }
}