import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StyleTheme {
    // Paleta de Cores "Streaming Dark"
    public static final Color BG_COLOR = new Color(20, 20, 20);       // Fundo principal
    public static final Color PANEL_COLOR = new Color(30, 30, 30);    // Fundo de cartões/paineis
    public static final Color ACCENT_COLOR = new Color(229, 9, 20);   // Vermelho Marca
    public static final Color TEXT_PRIMARY = new Color(255, 255, 255);
    public static final Color TEXT_SECONDARY = new Color(170, 170, 170);
    public static final Color INPUT_BG = new Color(50, 50, 50);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    public static void applyDarkTheme(JComponent component) {
        component.setBackground(BG_COLOR);
        component.setForeground(TEXT_PRIMARY);
    }

    public static JButton createButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isPrimary) {
            btn.setBackground(ACCENT_COLOR);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(INPUT_BG);
            btn.setForeground(Color.WHITE);
        }
        return btn;
    }

    public static JTextField createTextField() {
        JTextField field = new JTextField();
        field.setCaretColor(Color.WHITE);
        field.setBackground(INPUT_BG);
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,60,60)),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setCaretColor(Color.WHITE);
        field.setBackground(INPUT_BG);
        field.setForeground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,60,60)),
                new EmptyBorder(5, 10, 5, 10)));
        return field;
    }
}