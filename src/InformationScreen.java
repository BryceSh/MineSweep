import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class InformationScreen {

    JFrame main;

    public InformationScreen() {

        JFrame frame = new JFrame("Options");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        WindowListener listener = new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                frame.dispose();
                MineSweapPart.main(null);
            }
        };
        Container container = frame.getContentPane();
        JTextPane pane = new JTextPane();
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setItalic(attributeSet, true);
        StyleConstants.setForeground(attributeSet, Color.black);
        pane.setCharacterAttributes(attributeSet, true);
        pane.setText("This version of minesweeper was made by Bryce Sheridan. https://github.com/BryceSh/MineSweeper.\n" +
                "This was a fun project to work on because I learned a lot of new things that I never knew before. For instance, I learned about recursion " +
                "loops which overall are very fascinating. I've also learned a lot more about GUI development in Java and how to make cool games like this one." +
                "With the knowledge that I've gained form this program, I'll be able to make other games in the future.\n" +
                "Thanks for checking out my game. Enjoy!\n\nAlso, MacOS is stupid and displays dialog buttons left to right instead of windows and linux that display right to left." +
                "So because of that, that's why the buttons might be backwards if you are running MacOS");
        pane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(pane);
        container.add(scrollPane, BorderLayout.CENTER);
        frame.setSize(550, 300);
        frame.addWindowListener(listener); frame.setVisible(true);

    }


}
