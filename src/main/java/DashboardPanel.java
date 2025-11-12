// Conteúdo de: castello898/projetosd/ProjetoSD-9a86b08447559d0a9d789a0d9a6580c2916b1b58/src/main/java/DashboardPanel.java

import org.json.JSONObject;
import org.json.JSONArray; // Importar JSONArray
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

        // --- Botões de Usuário Comum (Já existentes) ---
        JButton viewProfileButton = new JButton("Ver Meus Dados (R)");
        JButton updatePasswordButton = new JButton("Atualizar Senha (U)");
        JButton deleteAccountButton = new JButton("Apagar Minha Conta (D)");

        // --- NOVOS Botões (Itens Avaliados) ---
        JButton listMoviesButton = new JButton("Listar Todos os Filmes (R)"); // Item (c, g)
        JButton listUsersButton = new JButton("Listar Usuários (ADM)");      // Item (f)
        JButton createMovieButton = new JButton("Criar Filme (ADM)");         // Item (b)
        JButton updateMovieButton = new JButton("Editar Filme (ADM)");      // Item (d)
        JButton deleteMovieButton = new JButton("Apagar Filme (ADM)");      // Item (e)

        JButton logoutButton = new JButton("Logout");

        // Adiciona botões de usuário
        actionsPanel.add(viewProfileButton);
        actionsPanel.add(updatePasswordButton);
        actionsPanel.add(deleteAccountButton);

        // Adiciona separador
        actionsPanel.add(new JSeparator());

        // Adiciona botões de ADM/Filmes
        actionsPanel.add(listMoviesButton);
        actionsPanel.add(listUsersButton);
        actionsPanel.add(createMovieButton);
        actionsPanel.add(updateMovieButton);
        actionsPanel.add(deleteMovieButton);

        // Adiciona separador
        actionsPanel.add(new JSeparator());

        actionsPanel.add(logoutButton);

        add(actionsPanel, BorderLayout.CENTER);

        // --- Ações dos Botões (Já existentes) ---

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
            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
            return response;
        }, "Logout"));

        // --- NOVAS Ações dos Botões (Itens Avaliados) ---

        // Item (c, g): Listar Filmes
        listMoviesButton.addActionListener(e -> executeNetworkTask(networkService::listAllMovies, "Listar Filmes"));

        // Item (f): Listar Usuários
        listUsersButton.addActionListener(e -> executeNetworkTask(networkService::listAllUsers, "Listar Usuários"));

        // Item (b): Criar Filme
        createMovieButton.addActionListener(e -> {
            // (Para um app real, seria melhor um JPanel customizado no JOptionPane)
            String titulo = JOptionPane.showInputDialog(this, "Título:");
            if (titulo == null) return;
            String diretor = JOptionPane.showInputDialog(this, "Diretor:");
            if (diretor == null) return;
            String ano = JOptionPane.showInputDialog(this, "Ano:");
            if (ano == null) return;
            String generos = JOptionPane.showInputDialog(this, "Gêneros (separados por vírgula):");
            if (generos == null) return;
            String sinopse = JOptionPane.showInputDialog(this, "Sinopse:");
            if (sinopse == null) return;

            executeNetworkTask(() -> networkService.createMovie(titulo, diretor, ano, generos, sinopse), "Criar Filme");
        });

        // Item (d): Editar Filme
        updateMovieButton.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "ID do filme a EDITAR:");
            if (id == null || id.trim().isEmpty()) return;

            String titulo = JOptionPane.showInputDialog(this, "Novo Título:");
            if (titulo == null) return;
            String diretor = JOptionPane.showInputDialog(this, "Novo Diretor:");
            if (diretor == null) return;
            String ano = JOptionPane.showInputDialog(this, "Novo Ano:");
            if (ano == null) return;
            String generos = JOptionPane.showInputDialog(this, "Novos Gêneros (separados por vírgula):");
            if (generos == null) return;
            String sinopse = JOptionPane.showInputDialog(this, "Nova Sinopse:");
            if (sinopse == null) return;

            executeNetworkTask(() -> networkService.updateMovie(id, titulo, diretor, ano, generos, sinopse), "Editar Filme");
        });

        // Item (e): Apagar Filme
        deleteMovieButton.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "ID do filme a APAGAR:");
            if (id != null && !id.trim().isEmpty()) {
                executeNetworkTask(() -> networkService.deleteMovie(id), "Apagar Filme");
            }
        });
    }

    /**
     * ATUALIZADO: Este método agora trata as respostas que contêm
     * arrays ("filmes" e "usuarios").
     */
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
                        // Trata sucesso
                        String successMessage = StatusCodeHandler.getMessage(status);

                        // --- LÓGICA ATUALIZADA ---
                        if (title.equals("Listar Filmes") && response.has("filmes")) {
                            showListPopup(response.getJSONArray("filmes"), title); // Mostra popup com lista
                        } else if (title.equals("Listar Usuários") && response.has("usuarios")) {
                            showListPopup(response.getJSONArray("usuarios"), title); // Mostra popup com lista
                        } else if (title.equals("Dados do Perfil") && response.has("usuario")) {
                            successMessage += "\n\nNome de Usuário: " + response.getString("usuario");
                            JOptionPane.showMessageDialog(DashboardPanel.this, successMessage, title, JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            // Sucesso genérico (Criar, Editar, Apagar, Logout, etc.)
                            if (response.has("mensagem")) {
                                successMessage += "\nDetalhe: " + response.getString("mensagem");
                            }
                            JOptionPane.showMessageDialog(DashboardPanel.this, successMessage, title, JOptionPane.INFORMATION_MESSAGE);
                        }
                        // --- FIM DA LÓGICA ATUALIZADA ---

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

    /**
     * NOVO MÉTODO: Helper para exibir um JSONArray em um popup com scroll.
     */
    private void showListPopup(JSONArray array, String title) {
        if (array.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum item encontrado.", title, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder listContent = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            // Formata o JSON para melhor legibilidade
            listContent.append(array.getJSONObject(i).toString(2));
            listContent.append("\n--------------------\n");
        }

        JTextArea textArea = new JTextArea(listContent.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400)); // Tamanho do popup
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }


    @FunctionalInterface
    interface NetworkTask {
        JSONObject execute() throws Exception;
    }

    public void updateUserInfo(String username) {
        welcomeLabel.setText("Bem-vindo, " + username + "!");
    }
}