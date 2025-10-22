import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {
    // ... (o início da sua classe permanece igual) ...

    public RegisterPanel(JPanel mainPanel, CardLayout cardLayout, NetworkService networkService) {
        // ... (toda a configuração do layout permanece a mesma) ...
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Cadastro de Usuário", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        JTextField loginField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JButton registerButton = new JButton("Cadastrar");
        JButton backToLoginButton = new JButton("Já tenho uma conta. Voltar.");
        backToLoginButton.setBorderPainted(false);

        gbc.gridwidth = 2; gbc.gridx = 0; gbc.gridy = 0; add(titleLabel, gbc);
        gbc.gridy++; gbc.gridwidth=1; add(new JLabel("Login:"), gbc);
        gbc.gridx = 1; add(loginField, gbc);
        gbc.gridy++; gbc.gridx = 0; add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; add(passwordField, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill=GridBagConstraints.NONE; add(registerButton, gbc);
        gbc.gridy++; add(backToLoginButton, gbc);

        registerButton.addActionListener(e -> {
            String login = loginField.getText();
            String password = new String(passwordField.getPassword());

            // Validação básica para não enviar campos vazios
            if (login.trim().isEmpty() || password.trim().isEmpty()) {
                JOptionPane.showMessageDialog(RegisterPanel.this,
                        "Login e senha não podem estar em branco.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
                return; // Interrompe a execução
            }

            new SwingWorker<JSONObject, Void>() {
                @Override
                protected JSONObject doInBackground() throws Exception {
                    return networkService.registerUser(login, password);
                }
                @Override
                protected void done() {
                    try {
                        JSONObject response = get();
                        int status = response.getInt("status");

                        if (status == 201) { // 201 Created
                            // Usa a mensagem de sucesso do nosso Handler
                            JOptionPane.showMessageDialog(RegisterPanel.this,
                                    StatusCodeHandler.getMessage(status)); // MUDANÇA AQUI
                            cardLayout.show(mainPanel, "LOGIN");
                        } else {
                            // ALTERAÇÃO: Usa o StatusCodeHandler para obter a mensagem de erro
                            String errorMessage = StatusCodeHandler.getMessage(status);

                            // Opcional: Se o servidor enviar um detalhe extra, podemos adicionar
                            if (response.has("mensagem")) {
                                errorMessage += "\nDetalhe: " + response.getString("mensagem");
                            }

                            JOptionPane.showMessageDialog(RegisterPanel.this,
                                    errorMessage, "Erro de Cadastro", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(RegisterPanel.this,
                                "Erro de comunicação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        backToLoginButton.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
    }
}