import org.json.JSONObject;
import org.json.JSONArray;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final NetworkService networkService;
    private JLabel welcomeLabel;

    // Cores inspiradas na Netflix
    private static final Color NETFLIX_RED = new Color(229, 9, 20);
    private static final Color NETFLIX_BACKGROUND = new Color(20, 20, 20);
    private static final Color NETFLIX_TEXT = Color.WHITE;

    public DashboardPanel(JPanel mainPanel, CardLayout cardLayout, NetworkService networkService) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        this.networkService = networkService;

        setLayout(new BorderLayout(10, 10));
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
        JButton listMoviesButton = createStyledButton("Listar Todos os Filmes"); // Item (c, g)
        JButton viewProfileButton = createStyledButton("Ver Meus Dados");
        JButton updatePasswordButton = createStyledButton("Atualizar Senha");
        JButton deleteAccountButton = createStyledButton("Apagar Minha Conta");
        JButton logoutButton = createStyledButton("Logout");
        styleButtonAsDestructive(deleteAccountButton);
        styleButtonAsPrimary(logoutButton, false);

        userPanel.add(listMoviesButton);
        userPanel.add(viewProfileButton);
        userPanel.add(updatePasswordButton);
        userPanel.add(deleteAccountButton);
        userPanel.add(new JSeparator());
        userPanel.add(logoutButton);

        // --- Botões de Admin ---
        // Adicionamos um botão de "Gerenciar Filmes" que lista os filmes com permissões de admin
        JButton adminListMoviesButton = createStyledButton("Gerenciar Filmes (Listar/Editar/Excluir)");
        JButton listUsersButton = createStyledButton("Listar Usuários");      // Item (f)
        JButton createMovieButton = createStyledButton("Criar Novo Filme");   // Item (b)
        // Removemos os botões antigos de "Editar" e "Apagar" isolados, pois agora faremos isso via lista ou botão de criar

        styleButtonAsPrimary(createMovieButton, true);

        adminPanel.add(createMovieButton);
        adminPanel.add(adminListMoviesButton); // Novo fluxo de edição via lista
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

        // Ação de Listar Filmes (Modo Leitura - Usuário Comum)
        listMoviesButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject response = networkService.listAllMovies();
            // Flag 'false' indica que NÃO é admin, então não mostra botões de editar/excluir
            SwingUtilities.invokeLater(() -> showElegantMovieList(response.optJSONArray("filmes"), false));
            return response;
        }, "Listar Filmes"));


        // --- Ações dos Botões (Admin) ---

        listUsersButton.addActionListener(e -> executeNetworkTask(networkService::listAllUsers, "Listar Usuários"));

        // Ação de Listar Filmes (Modo Admin - Com botões de edição)
        adminListMoviesButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject response = networkService.listAllMovies();
            // Flag 'true' indica que É admin
            SwingUtilities.invokeLater(() -> showElegantMovieList(response.optJSONArray("filmes"), true));
            return response;
        }, "Gerenciar Filmes"));

        // Ação de Criar Filme (Abre o novo Formulário)
        createMovieButton.addActionListener(e -> showMovieForm(null, null));
    }

    // =============================================================================================
    // NOVOS MÉTODOS DE UI (Formulários e Listas Melhoradas)
    // =============================================================================================

    /**
     * Exibe um formulário JDialog para Criar ou Editar um filme.
     * @param idToEdit ID do filme se for edição, ou null se for criação.
     * @param existingData Dados atuais do filme para preencher os campos (opcional).
     */
    private void showMovieForm(String idToEdit, JSONObject existingData) {
        boolean isEditing = (idToEdit != null);
        String titleWindow = isEditing ? "Editar Filme" : "Cadastrar Novo Filme";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), titleWindow, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 600);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 5, 5); // Espaçamento
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Componentes do Formulário
        JTextField titleField = new JTextField();
        JTextField directorField = new JTextField();
        JTextField yearField = new JTextField();
        JTextField genresField = new JTextField();
        JTextArea synopsisArea = new JTextArea(5, 20);
        synopsisArea.setLineWrap(true);
        synopsisArea.setWrapStyleWord(true);
        JScrollPane synopsisScroll = new JScrollPane(synopsisArea);

        // Preencher dados se for edição
        if (existingData != null) {
            titleField.setText(existingData.optString("titulo"));
            directorField.setText(existingData.optString("diretor"));
            yearField.setText(existingData.optString("ano"));
            synopsisArea.setText(existingData.optString("sinopse"));

            // Converter JSONArray de gêneros para String separada por vírgula
            JSONArray genresArray = existingData.optJSONArray("genero");
            if (genresArray != null) {
                List<String> genreList = new ArrayList<>();
                for(int i=0; i<genresArray.length(); i++) genreList.add(genresArray.getString(i));
                genresField.setText(String.join(", ", genreList));
            }
        } else if (isEditing) {
            // Se estamos editando mas não passamos o objeto (ex: botão antigo de ID), apenas o ID fica guardado
            // O ideal é sempre passar o objeto.
        }

        // Adicionando ao painel
        addFormField(formPanel, gbc, 0, "Título:", titleField);
        addFormField(formPanel, gbc, 1, "Diretor:", directorField);
        addFormField(formPanel, gbc, 2, "Ano:", yearField);
        addFormField(formPanel, gbc, 3, "Gêneros (separados por vírgula):", genresField);

        // Área de Sinopse (configuração especial de layout)
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Sinopse:"), gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        formPanel.add(synopsisScroll, gbc);

        // Botões de Ação
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(isEditing ? "Salvar Alterações" : "Criar Filme");
        JButton cancelButton = new JButton("Cancelar");

        styleButtonAsPrimary(saveButton, true);

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Lógica do Botão Salvar
        saveButton.addActionListener(ev -> {
            String t = titleField.getText();
            String d = directorField.getText();
            String a = yearField.getText();
            String g = genresField.getText();
            String s = synopsisArea.getText();

            // Validação simples
            if (t.isEmpty() || d.isEmpty() || a.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Preencha os campos obrigatórios.", "Erro", JOptionPane.WARNING_MESSAGE);
                return;
            }

            dialog.dispose(); // Fecha o formulário

            // Executa a tarefa de rede
            if (isEditing) {
                executeNetworkTask(() -> networkService.updateMovie(idToEdit, t, d, a, g, s), "Editar Filme");
            } else {
                executeNetworkTask(() -> networkService.createMovie(t, d, a, g, s), "Criar Filme");
            }
        });

        cancelButton.addActionListener(ev -> dialog.dispose());

        dialog.setVisible(true);
    }

    // Helper para adicionar label + campo no GridBagLayout
    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    /**
     * Versão atualizada do Listar Filmes que aceita flag de Admin.
     * Se isAdmin for true, mostra botões de Editar/Excluir nos cards.
     */
    private void showElegantMovieList(JSONArray movies, boolean isAdmin) {
        if (movies == null) return; // Proteção contra null

        JDialog movieDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Filmes - VoteFlix", true);
        movieDialog.setSize(900, 650);
        movieDialog.setLocationRelativeTo(this);
        movieDialog.setLayout(new BorderLayout());

        if (movies.isEmpty()) {
            movieDialog.add(new JLabel("Nenhum filme cadastrado.", SwingConstants.CENTER), BorderLayout.CENTER);
            movieDialog.setVisible(true);
            return;
        }

        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        gridPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        for (int i = 0; i < movies.length(); i++) {
            JSONObject movie = movies.getJSONObject(i);
            // Passamos a flag isAdmin para o criador do card
            JPanel movieCard = createMovieCard(movie, isAdmin, movieDialog);
            gridPanel.add(movieCard);
        }

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Scroll mais suave
        movieDialog.add(scrollPane, BorderLayout.CENTER);
        movieDialog.setVisible(true);
    }

    /**
     * Cria o card visual do filme. Agora inclui botões de Admin se necessário.
     */
    private JPanel createMovieCard(JSONObject movie, boolean isAdmin, JDialog parentDialog) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        String title = movie.optString("titulo", "Sem Título");
        String id = movie.optString("id", "?");
        String ano = movie.optString("ano", "----");

        JLabel titleLabel = new JLabel(String.format("<html><b>%s</b> (%s)</html>", title, ano));
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        card.add(titleLabel, BorderLayout.NORTH);

        String sinopse = movie.optString("sinopse", "Sinopse não disponível.");
        JTextArea sinopseArea = new JTextArea(sinopse);
        sinopseArea.setLineWrap(true);
        sinopseArea.setWrapStyleWord(true);
        sinopseArea.setEditable(false);
        sinopseArea.setFont(new Font("SansSerif", Font.ITALIC, 12));
        sinopseArea.setOpaque(false);
        card.add(new JScrollPane(sinopseArea), BorderLayout.CENTER);

        // Painel inferior: Detalhes + Botões de Ação
        JPanel bottomPanel = new JPanel(new BorderLayout());

        String diretor = movie.optString("diretor", "Desconhecido");
        JSONArray genArr = movie.optJSONArray("genero");
        String generos = genArr != null ? genArr.toString() : "[]";

        JLabel detailsLabel = new JLabel(String.format("<html><small>ID: %s<br>Diretor: %s<br>%s</small></html>", id, diretor, generos));
        bottomPanel.add(detailsLabel, BorderLayout.CENTER);

        // Se for admin, adiciona botões de Editar/Excluir
        if (isAdmin) {
            JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            JButton editBtn = new JButton("✏️"); // Ícone de lápis
            JButton delBtn = new JButton("🗑️"); // Ícone de lixo

            editBtn.setToolTipText("Editar Filme");
            delBtn.setToolTipText("Apagar Filme");
            styleButtonAsDestructive(delBtn);

            editBtn.addActionListener(e -> {
                // Abre o formulário de edição PREENCHIDO
                showMovieForm(id, movie);
                // Opcional: fechar a lista para forçar refresh, ou implementar refresh dinâmico
                parentDialog.dispose();
            });

            delBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(card, "Apagar '" + title + "'?", "Confirmar", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    executeNetworkTask(() -> networkService.deleteMovie(id), "Apagar Filme");
                    parentDialog.dispose(); // Fecha lista para atualizar
                }
            });

            adminActions.add(editBtn);
            adminActions.add(Box.createHorizontalStrut(5));
            adminActions.add(delBtn);

            bottomPanel.add(adminActions, BorderLayout.SOUTH);
        }

        card.add(bottomPanel, BorderLayout.SOUTH);
        return card;
    }

    // =============================================================================================
    // MÉTODOS AUXILIARES EXISTENTES (Mantidos ou levemente ajustados)
    // =============================================================================================

    private JPanel createTabPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }

    private void styleButtonAsPrimary(JButton button, boolean isPrimary) {
        if (isPrimary) {
            button.putClientProperty("JButton.buttonType", "roundRect");
            button.setBackground(NETFLIX_RED);
            button.setForeground(Color.WHITE);
        }
    }

    private void styleButtonAsDestructive(JButton button) {
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBackground(NETFLIX_RED);
        button.setForeground(Color.WHITE);
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
                    // Proteção se a resposta for nula (erro de socket tratado no service)
                    if (response == null) return;

                    String status = response.has("status") ? response.getString("status") : "500";

                    if (status.startsWith("2")) {
                        String successMessage = StatusCodeHandler.getMessage(status);

                        if (title.equals("Listar Filmes") && response.has("filmes")) {
                            // Este bloco foi movido para dentro do listener para passar a flag 'isAdmin' corretamente.
                            // Mas mantemos aqui como fallback se chamado genericamente.
                            // showElegantMovieList(response.getJSONArray("filmes"), false);
                        } else if (title.equals("Listar Usuários") && response.has("usuarios")) {
                            showElegantUserList(response.getJSONArray("usuarios"));
                        } else if (title.equals("Dados do Perfil") && response.has("usuario")) {
                            successMessage += "\n\nNome de Usuário: " + response.getString("usuario");
                            JOptionPane.showMessageDialog(DashboardPanel.this, successMessage, title, JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            if (response.has("mensagem")) {
                                successMessage += "\nDetalhe: " + response.getString("mensagem");
                            }
                            JOptionPane.showMessageDialog(DashboardPanel.this, successMessage, title, JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        String errorMessage = StatusCodeHandler.getMessage(status);
                        if (response.has("mensagem")) {
                            errorMessage += "\nDetalhe: " + response.getString("mensagem");
                        }
                        JOptionPane.showMessageDialog(DashboardPanel.this, errorMessage, "Erro em: " + title, JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Erro de comunicação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // (Mantido o método showElegantUserList e createUserCard do código original, sem alterações significativas)
    private void showElegantUserList(JSONArray users) {
        JDialog userDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Usuários - VoteFlix", true);
        userDialog.setSize(600, 500);
        userDialog.setLocationRelativeTo(this);
        userDialog.setLayout(new BorderLayout());

        if (users.isEmpty()) {
            userDialog.add(new JLabel("Nenhum usuário encontrado.", SwingConstants.CENTER), BorderLayout.CENTER);
            userDialog.setVisible(true);
            return;
        }
        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 15, 15));
        gridPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        for (int i = 0; i < users.length(); i++) {
            gridPanel.add(createUserCard(users.getJSONObject(i)));
        }
        userDialog.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        userDialog.setVisible(true);
    }

    // ... (Método createUserCard original permanece aqui) ...
    private JPanel createUserCard(JSONObject user) {
        // Implementação original do seu código (omitida aqui por brevidade, mas deve ser mantida)
        // Apenas para compilar o exemplo, vou colocar uma versão simplificada:
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        card.add(new JLabel(user.optString("nome")), BorderLayout.CENTER);
        // Botões de apagar user, etc.
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