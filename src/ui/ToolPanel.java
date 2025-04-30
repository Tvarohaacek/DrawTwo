package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import model.ColorPalette;
import model.LineStyle;
import model.ToolType;

import java.util.function.Consumer;

public class ToolPanel extends JPanel {
    private Color selectedColor = Color.WHITE;

    public ToolPanel(Consumer<Color> onColorChange,
                     Consumer<Integer> onThicknessChange,
                     Consumer<LineStyle> onStyleChange,
                     Consumer<ToolType> onToolChange) {
        setLayout(new GridBagLayout());
        setBackground(Color.DARK_GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);


        JPanel colorsPanel = new JPanel(new GridLayout(2, 4, 3, 3));
        colorsPanel.setBackground(Color.DARK_GRAY);
        colorsPanel.setBorder(BorderFactory.createTitledBorder("Barvy"));

        for (Color c : ColorPalette.BASIC_COLORS) {
            JButton btn = new JButton();
            btn.setBackground(c);
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(25, 25));
            btn.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            btn.addActionListener(e -> {
                selectedColor = c;
                onColorChange.accept(c);
            });

            colorsPanel.add(btn);
        }


        JPanel thicknessPanel = new JPanel(new BorderLayout());
        thicknessPanel.setBackground(Color.DARK_GRAY);
        thicknessPanel.setBorder(BorderFactory.createTitledBorder("Tloušťka"));

        JSlider thicknessSlider = new JSlider(1, 10, 1);
        thicknessSlider.setBackground(Color.DARK_GRAY);
        thicknessSlider.setMajorTickSpacing(3);
        thicknessSlider.setPaintTicks(true);
        thicknessSlider.setPaintLabels(true);
        thicknessSlider.addChangeListener(e -> {
            onThicknessChange.accept(thicknessSlider.getValue());
        });
        thicknessPanel.add(thicknessSlider, BorderLayout.CENTER);


        JPanel stylePanel = new JPanel(new BorderLayout());
        stylePanel.setBackground(Color.DARK_GRAY);
        stylePanel.setBorder(BorderFactory.createTitledBorder("Styl čáry"));

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
        stylePanel.add(styleCombo, BorderLayout.CENTER);


        JPanel toolPanel = new JPanel(new GridLayout(2, 4, 3, 3));
        toolPanel.setBackground(Color.DARK_GRAY);
        toolPanel.setBorder(BorderFactory.createTitledBorder("Nástroj"));

        JToggleButton lineTool = new JToggleButton("Čára");
        JToggleButton rectTool = new JToggleButton("Obdélník");
        JToggleButton circleTool = new JToggleButton("Kruh");
        JToggleButton polyTool = new JToggleButton("Polygon");
        JToggleButton fillTool = new JToggleButton("Výplň");
        JToggleButton selectionTool = new JToggleButton("Výběr");
        JToggleButton brushTool = new JToggleButton("Štětec");
        JToggleButton eraserTool = new JToggleButton("Guma");

        lineTool.setSelected(true);

        ButtonGroup toolGroup = new ButtonGroup();
        toolGroup.add(lineTool);
        toolGroup.add(rectTool);
        toolGroup.add(circleTool);
        toolGroup.add(polyTool);
        toolGroup.add(fillTool);
        toolGroup.add(selectionTool);
        toolGroup.add(brushTool);
        toolGroup.add(eraserTool);

        lineTool.addActionListener(e -> onToolChange.accept(ToolType.LINE));
        rectTool.addActionListener(e -> onToolChange.accept(ToolType.RECTANGLE));
        circleTool.addActionListener(e -> onToolChange.accept(ToolType.CIRCLE));
        polyTool.addActionListener(e -> onToolChange.accept(ToolType.POLYGON));
        fillTool.addActionListener(e -> onToolChange.accept(ToolType.FILL));
        selectionTool.addActionListener(e -> onToolChange.accept(ToolType.SELECTION));
        brushTool.addActionListener(e -> onToolChange.accept(ToolType.BRUSH));
        eraserTool.addActionListener(e -> onToolChange.accept(ToolType.ERASER));

        toolPanel.add(lineTool);
        toolPanel.add(rectTool);
        toolPanel.add(circleTool);
        toolPanel.add(polyTool);
        toolPanel.add(fillTool);
        toolPanel.add(selectionTool);
        toolPanel.add(brushTool);
        toolPanel.add(eraserTool);



        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.3;
        gbc.weighty = 0.5;
        add(colorsPanel, gbc);


        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        gbc.weighty = 0.5;
        add(thicknessPanel, gbc);


        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.2;
        gbc.weighty = 1.0;
        add(stylePanel, gbc);


        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        add(toolPanel, gbc);
    }
}