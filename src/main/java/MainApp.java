import com.formdev.flatlaf.FlatDarkLaf; // Importar o Look and Feel
import javax.swing.*;
import java.awt.*;

public class MainApp {

    public static void main(String[] args) {
        // Configura o Look and Feel escuro ANTES de qualquer componente Swing
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Falha ao inicializar o FlatLaf. Usando L&F padrão.");
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("VoteFlix Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 600); // Ajustei o tamanho para o novo layout

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            NetworkService networkService = new NetworkService();

            // Cria os painéis (telas)
            // OBS: O FlatLaf vai automaticamente aplicar o tema escuro a todos!
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