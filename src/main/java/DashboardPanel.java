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

        // Painel com os botões de ação
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

        // (b) Ler dados do cadastro
        viewProfileButton.addActionListener(e -> executeNetworkTask(networkService::viewProfile, "Dados do Perfil"));

        // (e) Atualizar senha
        updatePasswordButton.addActionListener(e -> {
            String newPassword = JOptionPane.showInputDialog(this, "Digite a nova senha:", "Atualizar Senha", JOptionPane.PLAIN_MESSAGE);
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                executeNetworkTask(() -> networkService.updateUserPassword(newPassword), "Atualização de Senha");
            }
        });

        // (f) Apagar conta
        deleteAccountButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Tem certeza que deseja apagar sua conta? Esta ação é irreversível.",
                    "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                executeNetworkTask(() -> {
                    JSONObject response = networkService.deleteUser();
                    // A lógica de navegação só ocorre se o status for 200 (OK)
                    if (response.getInt("status") == 200) {
                        SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
                    }
                    return response;
                }, "Exclusão de Conta");
            }
        });

        // (d) Logout
        logoutButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject response = networkService.logoutUser();
            // Navega para a tela de conexão independentemente da resposta
            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
            return response;
        }, "Logout"));
    }

    // Método auxiliar para executar tarefas de rede e mostrar o resultado
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
                    int status = response.getInt("status");

                    // Verifica se o status é de sucesso (2xx)
                    if (status >= 200 && status < 300) {
                        String successMessage = StatusCodeHandler.getMessage(status);

                        // Para "Ver Perfil", é útil mostrar os dados retornados
                        if (title.equals("Dados do Perfil") && response.has("usuario")) {

                            // CORREÇÃO AQUI:
                            // O servidor envia o nome do usuário como String, não como Objeto.
                            // Trocamos response.getJSONObject("usuario").toString(4)
                            // por response.getString("usuario")
                            successMessage += "\n\nNome de Usuário: " + response.getString("usuario");

                        } else if (response.has("mensagem")) {
                            successMessage += "\nDetalhe: " + response.getString("mensagem");
                        }

                        JOptionPane.showMessageDialog(DashboardPanel.this,
                                successMessage,
                                title, // Título da ação
                                JOptionPane.INFORMATION_MESSAGE); // Ícone de informação

                    } else {
                        // É um erro (4xx, 5xx)
                        String errorMessage = StatusCodeHandler.getMessage(status);
                        if (response.has("mensagem")) {
                            errorMessage += "\nDetalhe: " + response.getString("mensagem");
                        }
                        JOptionPane.showMessageDialog(DashboardPanel.this,
                                errorMessage,
                                "Erro em: " + title, // Título da ação
                                JOptionPane.ERROR_MESSAGE); // Ícone de erro
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

    // Interface funcional para simplificar a chamada do SwingWorker
    @FunctionalInterface
    interface NetworkTask {
        JSONObject execute() throws Exception;
    }

    // Método para atualizar a UI quando o usuário faz login
    public void updateUserInfo(String username) {
        welcomeLabel.setText("Bem-vindo, " + username + "!");
    }
}