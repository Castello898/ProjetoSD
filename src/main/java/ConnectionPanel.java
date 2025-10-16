import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ConnectionPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final NetworkService networkService;

    public ConnectionPanel(JPanel mainPanel, CardLayout cardLayout, NetworkService networkService) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        this.networkService = networkService;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Conectar ao Servidor", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        JTextField ipField = new JTextField("127.0.0.1", 20);
        JTextField portField = new JTextField("12345", 20);
        JButton connectButton = new JButton("Conectar");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("IP do Servidor:"), gbc);
        gbc.gridx = 1;
        add(ipField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Porta:"), gbc);
        gbc.gridx = 1;
        add(portField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(connectButton, gbc);

        connectButton.addActionListener(e -> {
            String ip = ipField.getText();
            String portStr = portField.getText();

            // Ação de conectar executada em uma thread separada para não travar a UI
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    int port = Integer.parseInt(portStr);
                    networkService.connect(ip, port);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Pega exceções do doInBackground
                        JOptionPane.showMessageDialog(ConnectionPanel.this, "Conexão estabelecida com sucesso!");
                        cardLayout.show(mainPanel, "LOGIN");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ConnectionPanel.this,
                                "Erro ao conectar: " + ex.getCause().getMessage(),
                                "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
    }
}