import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final NetworkService networkService;
    private JLabel welcomeLabel;

    public DashboardPanel(JPanel mainPanel, CardLayout cardLayout, NetworkService networkService) {

        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        this.networkService = networkService;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        welcomeLabel = new JLabel("Bem-vindo!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(welcomeLabel, BorderLayout.NORTH);

        // Botões
        JPanel actionsPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        JButton viewProfileButton = new JButton("Ver Meus Dados (R)");
        JButton updatePasswordButton = new JButton("Atualizar Senha (U)");
        JButton deleteAccountButton = new JButton("Apagar Minha Conta (D)");
        JButton logoutButton = new JButton("Logout");

        actionsPanel.add(viewProfileButton);
        actionsPanel.add(updatePasswordButton);
        actionsPanel.add(deleteAccountButton);
        actionsPanel.add(logoutButton);

        add(actionsPanel, BorderLayout.CENTER);

        // --- Ações dos Botões ---

        //Ler dados do cadastro
        viewProfileButton.addActionListener(e -> executeNetworkTask(networkService::viewProfile, "Dados do Perfil"));

        //Atualizar senha
        updatePasswordButton.addActionListener(e -> {
            String newPassword = JOptionPane.showInputDialog(this, "Digite a nova senha:", "Atualizar Senha", JOptionPane.PLAIN_MESSAGE);
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                executeNetworkTask(() -> networkService.updateUserPassword(newPassword), "Atualização de Senha");
            }
        });

        //Apagar conta
        deleteAccountButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Tem certeza que deseja apagar sua conta? Esta ação é irreversível.",
                    "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                executeNetworkTask(() -> {
                    JSONObject response = networkService.deleteUser();

                    // ALTERAÇÃO: Lendo e comparando como String
                    if (response.getString("status").equals("200")) {
                        SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
                    }
                    return response;
                }, "Exclusão de Conta");
            }
        });

        //Logout
        logoutButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject response = networkService.logoutUser();
            // Navega para a tela de conexão independentemente da resposta
            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
            return response;
        }, "Logout"));
    }


    private void executeNetworkTask(NetworkTask task, String title) {
        new SwingWorker<JSONObject, Void>() {
            @Override
            protected JSONObject doInBackground() throws Exception {
                return task.execute();
            }
            @Override
            protected void done() {
                try {
                    JSONObject response = get();
                    String status = response.getString("status");

                    if (status.startsWith("2")) {
                        String successMessage = StatusCodeHandler.getMessage(status);


                        if (title.equals("Dados do Perfil") && response.has("usuario")) {

                            successMessage += "\n\nNome de Usuário: " + response.getString("usuario");

                        } else if (response.has("mensagem")) {
                            successMessage += "\nDetalhe: " + response.getString("mensagem");
                        }

                        JOptionPane.showMessageDialog(DashboardPanel.this,
                                successMessage,
                                title,
                                JOptionPane.INFORMATION_MESSAGE);

                    } else {
                        // É erro
                        String errorMessage = StatusCodeHandler.getMessage(status);
                        if (response.has("mensagem")) {
                            errorMessage += "\nDetalhe: " + response.getString("mensagem");
                        }
                        JOptionPane.showMessageDialog(DashboardPanel.this,
                                errorMessage,
                                "Erro em: " + title,
                                JOptionPane.ERROR_MESSAGE);
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this,
                            "Erro de comunicação: " + ex.getMessage(),
                            "Erro em: " + title,
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }


    @FunctionalInterface
    interface NetworkTask {
        JSONObject execute() throws Exception;
    }


    public void updateUserInfo(String username) {
        welcomeLabel.setText("Bem-vindo, " + username + "!");
    }
}