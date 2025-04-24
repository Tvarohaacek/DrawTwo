package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import model.ColorPalette;
import model.LineStyle;
import model.ToolType;

import java.util.function.Consumer;

/**
 * Panel s nástroji – v této fázi pouze výběr barvy.
 */
public class ToolPanel extends JPanel {
    private Color selectedColor = Color.WHITE;

    public ToolPanel(Consumer<Color> onColorChange,
                     Consumer<Integer> onThicknessChange,
                     Consumer<LineStyle> onStyleChange,
                     Consumer<ToolType> onToolChange) {
        this.setLayout(new BorderLayout());
        this.setBackground(Color.DARK_GRAY);

        JPanel colorsPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        colorsPanel.setBackground(Color.DARK_GRAY);
        colorsPanel.setBorder(BorderFactory.createTitledBorder("Barvy"));

        String[] styles = {"Plná", "Přerušovaná", "Tečkovaná"};
        JComboBox<String> styleCombo = new JComboBox<>(styles);
        styleCombo.addActionListener(e -> {
            String selected = (String) styleCombo.getSelectedItem();
            switch (selected) {
                case "Plná" -> onStyleChange.accept(LineStyle.SOLID);
                case "Přerušovaná" -> onStyleChange.accept(LineStyle.DASHED);
                case "Tečkovaná" -> onStyleChange.accept(LineStyle.DOTTED);
            }
        });

        for (Color c : ColorPalette.BASIC_COLORS) {
            JButton btn = new JButton();
            btn.setBackground(c);
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(40, 40));
            btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            btn.addActionListener(e -> {
                selectedColor = c;
                onColorChange.accept(c);
            });

            colorsPanel.add(btn);
        }

        JPanel thicknessPanel = new JPanel();
        thicknessPanel.setLayout(new BorderLayout());
        thicknessPanel.setBackground(Color.DARK_GRAY);
        thicknessPanel.setBorder(BorderFactory.createTitledBorder("Tloušťka čáry"));



        JSlider thicknessSlider = new JSlider(1, 10, 1);
        thicknessSlider.setMajorTickSpacing(1);
        thicknessSlider.setPaintTicks(true);
        thicknessSlider.setPaintLabels(true);
        thicknessSlider.addChangeListener(e -> {
            onThicknessChange.accept(thicknessSlider.getValue());
        });
        thicknessPanel.add(thicknessSlider, BorderLayout.CENTER);

        //Panel Stylu čáry
        JPanel stylePanel = new JPanel();
        stylePanel.setLayout(new BorderLayout());
        stylePanel.setBackground(Color.DARK_GRAY);
        stylePanel.setBorder(BorderFactory.createTitledBorder("Styl čáry"));
        stylePanel.add(styleCombo, BorderLayout.CENTER);

        //Panel na možnosti tvaru
        JPanel toolPanel = new JPanel();
        toolPanel.setLayout(new GridLayout(1, 2));
        toolPanel.setBackground(Color.DARK_GRAY);
        toolPanel.setBorder(BorderFactory.createTitledBorder("Nástroj"));

        JToggleButton lineTool = new JToggleButton("Čára");
        JToggleButton rectTool = new JToggleButton("Obdélník");
        JToggleButton circleTool = new JToggleButton("Kruh");
        JToggleButton polyTool = new JToggleButton("Polygon");
        JToggleButton fillTool = new JToggleButton("Výplň");
        JToggleButton selectionTool = new JToggleButton("Výběr");
        lineTool.setSelected(true);

        ButtonGroup toolGroup = new ButtonGroup();
        toolGroup.add(lineTool);
        toolGroup.add(rectTool);
        toolGroup.add(circleTool);
        toolGroup.add(polyTool);
        toolGroup.add(fillTool);
        toolGroup.add(selectionTool);

        lineTool.addActionListener(e -> onToolChange.accept(ToolType.LINE));
        rectTool.addActionListener(e -> onToolChange.accept(ToolType.RECTANGLE));
        circleTool.addActionListener(e -> onToolChange.accept(ToolType.CIRCLE));
        polyTool.addActionListener(e -> onToolChange.accept(ToolType.POLYGON));
        fillTool.addActionListener(e -> onToolChange.accept(ToolType.FILL));
        selectionTool.addActionListener(e -> onToolChange.accept(ToolType.SELECTION));

        toolPanel.add(lineTool);
        toolPanel.add(rectTool);
        toolPanel.add(circleTool);
        toolPanel.add(polyTool);
        toolPanel.add(fillTool);
        toolPanel.add(selectionTool);

        JPanel combined = new JPanel(new GridLayout(1, 2));
        combined.setBackground(Color.DARK_GRAY);
        combined.add(colorsPanel);
        combined.add(thicknessPanel);
        combined.add(stylePanel);
        combined.add(toolPanel);

        this.add(combined, BorderLayout.CENTER);
    }
}