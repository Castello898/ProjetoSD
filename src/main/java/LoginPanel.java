import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    // ... (mesma estrutura de construtor do ConnectionPanel) ...

    public LoginPanel(JPanel mainPanel, CardLayout cardLayout, NetworkService networkService) {
        // ... (configuração do layout similar ao ConnectionPanel) ...
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Componentes da UI
        JLabel titleLabel = new JLabel("Login - VoteFlix", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        JTextField loginField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Não tenho uma conta. Cadastrar.");
        registerButton.setBorderPainted(false);
        registerButton.setFocusPainted(false);
        registerButton.setContentAreaFilled(false);

        // Adicionando componentes ao painel
        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 0; add(titleLabel, gbc);
        gbc.gridwidth = 1; gbc.gridy++; gbc.gridx = 0; add(new JLabel("Login:"), gbc);
        gbc.gridx = 1; add(loginField, gbc);
        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; add(passwordField, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; add(loginButton, gbc);
        gbc.gridy++; add(registerButton, gbc);

        loginButton.addActionListener(e -> {
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());

            new SwingWorker<JSONObject, Void>() {
                @Override
                protected JSONObject doInBackground() throws Exception {
                    return networkService.loginUser(login, password);
                }
                @Override
                protected void done() {
                    try {
                        JSONObject response = get();
                        if (response.getInt("status") == 200) {
                            // Passa os dados do usuário para o Dashboard
                            DashboardPanel dashboard = (DashboardPanel) mainPanel.getComponent(3);
                            dashboard.updateUserInfo(login); // Atualiza a UI com os dados do usuário
                            cardLayout.show(mainPanel, "DASHBOARD");
                        } else {
                            JOptionPane.showMessageDialog(LoginPanel.this,
                                    response.getString("mensagem"), "Erro de Login", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(LoginPanel.this,
                                "Erro de comunicação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        registerButton.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
    }
}