import javax.swing.*;
import java.awt.*;

public class MainApp {

    public static void main(String[] args) {
        // Tenta aplicar um Look and Feel mais moderno
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.Nimbus.LookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("VoteFlix Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(450, 500);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            NetworkService networkService = new NetworkService();

            // Cria os painéis (telas)
            ConnectionPanel connectionPanel = new ConnectionPanel(mainPanel, cardLayout, networkService);
            LoginPanel loginPanel = new LoginPanel(mainPanel, cardLayout, networkService);
            RegisterPanel registerPanel = new RegisterPanel(mainPanel, cardLayout, networkService);
            DashboardPanel dashboardPanel = new DashboardPanel(mainPanel, cardLayout, networkService);

            // Adiciona as telas ao gerenciador de layout
            mainPanel.add(connectionPanel, "CONNECTION");
            mainPanel.add(loginPanel, "LOGIN");
            mainPanel.add(registerPanel, "REGISTER");
            mainPanel.add(dashboardPanel, "DASHBOARD");

            frame.add(mainPanel);
            frame.setLocationRelativeTo(null); // Centraliza na tela
            frame.setVisible(true);
        });
    }
}