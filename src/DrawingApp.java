import javax.swing.*;
import java.awt.*;
import ui.DrawingPanel;
import ui.ToolPanel;


public class DrawingApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("DrawTwo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            DrawingPanel drawingPanel = new DrawingPanel(800, 600);
            ToolPanel toolPanel = new ToolPanel(
                    drawingPanel::setCurrentColor,
                    drawingPanel::setCurrentThickness,
                    drawingPanel::setCurrentStyle,
                    drawingPanel::setCurrentTool
                    );

            frame.add(toolPanel, BorderLayout.NORTH);
            frame.add(drawingPanel, BorderLayout.CENTER);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}


