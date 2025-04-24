import javax.swing.*;
import java.awt.*;
import ui.DrawingPanel;
import ui.ToolPanel;

/**
 * Hlavní třída pro spuštění aplikace.
 */
public class DrawingApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Jednoduchý kreslící program");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            DrawingPanel drawingPanel = new DrawingPanel(800, 600);
            ToolPanel toolPanel = new ToolPanel(
                    drawingPanel::setCurrentColor,
                    drawingPanel::setCurrentThickness,
                    drawingPanel::setCurrentStyle,
                    drawingPanel::setCurrentTool
                    );

            frame.add(toolPanel, BorderLayout.NORTH);  // panel nástrojů nahoře
            frame.add(drawingPanel, BorderLayout.CENTER);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}


