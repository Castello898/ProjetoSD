import org.json.JSONObject;
import org.json.JSONArray;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final NetworkService networkService;
    private JLabel welcomeLabel;

    // Cores inspiradas na Netflix
    private static final Color NETFLIX_RED = new Color(229, 9, 20);
    private static final Color NETFLIX_BACKGROUND = new Color(20, 20, 20);
    private static final Color NETFLIX_PANEL_BACKGROUND = new Color(30, 30, 30);
    private static final Color NETFLIX_TEXT = Color.WHITE;

    public DashboardPanel(JPanel mainPanel, CardLayout cardLayout, NetworkService networkService) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        this.networkService = networkService;

        setLayout(new BorderLayout(10, 10));
        // O FlatLaf já define o fundo, mas podemos forçar se quisermos
        // setBackground(NETFLIX_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        welcomeLabel = new JLabel("Bem-vindo!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        welcomeLabel.setForeground(NETFLIX_TEXT);
        add(welcomeLabel, BorderLayout.NORTH);

        // --- Criação das Abas ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

        // --- Painel da Aba "Minha Conta" (Usuário) ---
        JPanel userPanel = createTabPanel();
        tabbedPane.addTab("Minha Conta", userPanel);

        // --- Painel da Aba "Administração" (Admin) ---
        JPanel adminPanel = createTabPanel();
        tabbedPane.addTab("Administração", adminPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // --- Botões de Usuário Comum ---
        JButton listMoviesButton = createStyledButton("Listar Todos os Filmes (R)"); // Item (c, g)
        JButton viewProfileButton = createStyledButton("Ver Meus Dados (R)");
        JButton updatePasswordButton = createStyledButton("Atualizar Senha (U)");
        JButton deleteAccountButton = createStyledButton("Apagar Minha Conta (D)");
        JButton logoutButton = createStyledButton("Logout");
        styleButtonAsDestructive(deleteAccountButton); // Deixa o botão de apagar vermelho
        styleButtonAsPrimary(logoutButton, false); // Botão de logout menos destacado


        userPanel.add(listMoviesButton);
        userPanel.add(viewProfileButton);
        userPanel.add(updatePasswordButton);
        userPanel.add(deleteAccountButton);
        userPanel.add(new JSeparator());
        userPanel.add(logoutButton);


        // --- Botões de Admin ---
        JButton listUsersButton = createStyledButton("Listar Usuários (ADM)");      // Item (f)
        JButton createMovieButton = createStyledButton("Criar Filme (ADM)");         // Item (b)
        JButton updateMovieButton = createStyledButton("Editar Filme (ADM)");      // Item (d)
        JButton deleteMovieButton = createStyledButton("Apagar Filme (ADM)");      // Item (e)
        styleButtonAsPrimary(createMovieButton, true); // Destaca o botão de "Criar"

        adminPanel.add(createMovieButton);
        adminPanel.add(updateMovieButton);
        adminPanel.add(deleteMovieButton);
        adminPanel.add(new JSeparator());
        adminPanel.add(listUsersButton);


        // --- Ações dos Botões (Usuário) ---
        viewProfileButton.addActionListener(e -> executeNetworkTask(networkService::viewProfile, "Dados do Perfil"));

        updatePasswordButton.addActionListener(e -> {
            String newPassword = JOptionPane.showInputDialog(this, "Digite a nova senha:", "Atualizar Senha", JOptionPane.PLAIN_MESSAGE);
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                executeNetworkTask(() -> networkService.updateUserPassword(newPassword), "Atualização de Senha");
            }
        });

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

        logoutButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject response = networkService.logoutUser();
            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
            return response;
        }, "Logout"));

        listMoviesButton.addActionListener(e -> executeNetworkTask(networkService::listAllMovies, "Listar Filmes"));

        // --- Ações dos Botões (Admin) ---
        listUsersButton.addActionListener(e -> executeNetworkTask(networkService::listAllUsers, "Listar Usuários"));

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

        updateMovieButton.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "ID do filme a EDITAR:");
            if (id == null || id.trim().isEmpty()) return;
            // ... (resto dos JOptionPanes)
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

        deleteMovieButton.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "ID do filme a APAGAR:");
            if (id != null && !id.trim().isEmpty()) {
                executeNetworkTask(() -> networkService.deleteMovie(id), "Apagar Filme");
            }
        });
    }

    // Método auxiliar para criar os painéis das abas
    private JPanel createTabPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        // O FlatLaf cuida da cor de fundo do painel da aba
        return panel;
    }

    // Método auxiliar para criar botões já estilizados
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Altura fixa, largura máxima
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // Centraliza horizontalmente
        return button;
    }

    // Estilo especial para botões primários (ex: Criar)
    private void styleButtonAsPrimary(JButton button, boolean isPrimary) {
        if (isPrimary) {
            button.putClientProperty("JButton.buttonType", "roundRect"); // Propriedade do FlatLaf
            button.setBackground(NETFLIX_RED);
            button.setForeground(Color.WHITE);
        }
    }

    // Estilo especial para botões destrutivos (ex: Apagar)
    private void styleButtonAsDestructive(JButton button) {
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBackground(NETFLIX_RED);
        button.setForeground(Color.WHITE);
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
                            // CHAMA O NOVO MÉTODO ELEGANTE!
                            showElegantMovieList(response.getJSONArray("filmes"));
                        } else if (title.equals("Listar Usuários") && response.has("usuarios")) {
                            showListPopup(response.getJSONArray("usuarios"), title); // Mantém o popup antigo para usuários
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
                    ex.printStackTrace(); // Bom para debugar
                    JOptionPane.showMessageDialog(DashboardPanel.this,
                            "Erro de comunicação: " + ex.getMessage(),
                            "Erro em: " + title,
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Helper antigo para exibir um JSONArray em um popup com scroll.
     * (Mantido para "Listar Usuários")
     */
    private void showListPopup(JSONArray array, String title) {
        if (array.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum item encontrado.", title, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder listContent = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            listContent.append(array.getJSONObject(i).toString(2));
            listContent.append("\n--------------------\n");
        }

        JTextArea textArea = new JTextArea(listContent.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400)); // Tamanho do popup
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * NOVO MÉTODO: Mostra a lista de filmes em um grid de cards.
     */
    private void showElegantMovieList(JSONArray movies) {
        // Cria uma nova janela (JDialog) para mostrar os filmes
        JDialog movieDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Filmes - VoteFlix", true);
        movieDialog.setSize(800, 600);
        movieDialog.setLocationRelativeTo(this);
        movieDialog.setLayout(new BorderLayout());

        if (movies.isEmpty()) {
            JLabel noMoviesLabel = new JLabel("Nenhum filme cadastrado.", SwingConstants.CENTER);
            noMoviesLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            movieDialog.add(noMoviesLabel, BorderLayout.CENTER);
            movieDialog.setVisible(true);
            return;
        }

        // Painel que conterá os cards, com um grid layout (3 colunas)
        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 15, 15)); // 0 linhas, 3 colunas, 15px de gap
        gridPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        // O FlatLaf cuida da cor

        // Itera sobre os filmes e cria um "card" para cada um
        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.getJSONObject(i);
            JPanel movieCard = createMovieCard(movie);
            gridPanel.add(movieCard);
        }

        // Adiciona o grid de cards a um painel de scroll
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        movieDialog.add(scrollPane, BorderLayout.CENTER);
        movieDialog.setVisible(true);
    }

    /**
     * NOVO MÉTODO: Cria um painel (card) para um único filme.
     */
    private JPanel createMovieCard(JSONObject movie) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1), // Borda fina
                new EmptyBorder(10, 10, 10, 10) // Espaçamento interno
        ));
        // O FlatLaf cuida da cor de fundo

        // Título
        String title = movie.optString("titulo", "Sem Título");
        String ano = movie.optString("ano", "----");
        JLabel titleLabel = new JLabel(String.format("<html><b>%s</b> (%s)</html>", title, ano));
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        card.add(titleLabel, BorderLayout.NORTH);

        // Sinopse
        String sinopse = movie.optString("sinopse", "Sinopse não disponível.");
        JTextArea sinopseArea = new JTextArea(sinopse);
        sinopseArea.setLineWrap(true);
        sinopseArea.setWrapStyleWord(true);
        sinopseArea.setEditable(false);
        sinopseArea.setFont(new Font("SansSerif", Font.ITALIC, 12));
        sinopseArea.setOpaque(false); // Fundo transparente
        card.add(new JScrollPane(sinopseArea), BorderLayout.CENTER); // Scroll se a sinopse for longa

        // Detalhes (Diretor, Gênero)
        String diretor = movie.optString("diretor", "Desconhecido");
        String generos = "Gêneros: " + movie.optJSONArray("genero").toString();
        JLabel detailsLabel = new JLabel(String.format("<html>Diretor: %s<br>%s</html>", diretor, generos));
        detailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        card.add(detailsLabel, BorderLayout.SOUTH);

        return card;
    }


    @FunctionalInterface
    interface NetworkTask {
        JSONObject execute() throws Exception;
    }

    public void updateUserInfo(String username) {
        welcomeLabel.setText("Bem-vindo, " + username + "!");
    }
}