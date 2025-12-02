import org.json.JSONObject;
import org.json.JSONArray;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final NetworkService networkService;
    private JLabel welcomeLabel;
    private String currentUsername;
    private boolean isAdmin;

    // Cores e Estilos
    private static final Color NETFLIX_RED = new Color(229, 9, 20);
    private static final Color NETFLIX_BACKGROUND = new Color(20, 20, 20);
    private static final Color NETFLIX_TEXT = Color.WHITE;
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font TEXT_FONT = new Font("SansSerif", Font.PLAIN, 14);

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

        // --- Abas ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel userPanel = createTabPanel();
        tabbedPane.addTab("Minha Conta", userPanel);

        JPanel adminPanel = createTabPanel();
        tabbedPane.addTab("Administração", adminPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // --- Botões Usuário ---
        JButton listMoviesButton = createStyledButton("Catálogo de Filmes (Ver & Avaliar)");
        JButton viewProfileButton = createStyledButton("Ver Meus Dados");
        JButton myReviewsButton = createStyledButton("Minhas Avaliações");
        JButton updatePasswordButton = createStyledButton("Atualizar Senha");
        JButton deleteAccountButton = createStyledButton("Apagar Minha Conta");
        JButton logoutButton = createStyledButton("Logout");
        styleButtonAsDestructive(deleteAccountButton);
        styleButtonAsPrimary(logoutButton, false);

        userPanel.add(listMoviesButton);
        userPanel.add(myReviewsButton); // Novo botão
        userPanel.add(viewProfileButton);
        userPanel.add(updatePasswordButton);
        userPanel.add(deleteAccountButton);
        userPanel.add(new JSeparator());
        userPanel.add(logoutButton);

        // --- Botões Admin ---
        JButton createMovieButton = createStyledButton("Cadastrar Novo Filme");
        JButton adminListMoviesButton = createStyledButton("Gerenciar Filmes (Editar/Excluir)");
        JButton listUsersButton = createStyledButton("Gerenciar Usuários");

        styleButtonAsPrimary(createMovieButton, true);

        adminPanel.add(createMovieButton);
        adminPanel.add(adminListMoviesButton);
        adminPanel.add(new JSeparator());
        adminPanel.add(listUsersButton);

        // --- Listeners Usuário ---

        // Ao listar filmes, abre a lista. O modo "Admin" define se aparecem os botões de editar o filme na lista.
        listMoviesButton.addActionListener(e -> fetchAndShowMovies(false));

        myReviewsButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject response = networkService.listMyReviews();
            SwingUtilities.invokeLater(() -> showReviewsList(response.optJSONArray("reviews"), "Minhas Avaliações"));
            return response;
        }, "Listar Minhas Avaliações"));

        viewProfileButton.addActionListener(e -> executeNetworkTask(networkService::viewProfile, "Dados do Perfil"));

        updatePasswordButton.addActionListener(e -> {
            String newPass = JOptionPane.showInputDialog(this, "Nova senha:", "Atualizar Senha", JOptionPane.PLAIN_MESSAGE);
            if (newPass != null && !newPass.trim().isEmpty()) {
                executeNetworkTask(() -> networkService.updateUserPassword(newPass), "Atualizar Senha");
            }
        });

        deleteAccountButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Tem certeza?", "Apagar Conta", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                executeNetworkTask(() -> {
                    JSONObject r = networkService.deleteUser();
                    if ("200".equals(r.optString("status"))) SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
                    return r;
                }, "Excluir Conta");
            }
        });

        logoutButton.addActionListener(e -> executeNetworkTask(() -> {
            JSONObject r = networkService.logoutUser();
            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "CONNECTION"));
            return r;
        }, "Logout"));

        // --- Listeners Admin ---
        adminListMoviesButton.addActionListener(e -> fetchAndShowMovies(true));
        createMovieButton.addActionListener(e -> showMovieForm(null, null));
        listUsersButton.addActionListener(e -> executeNetworkTask(networkService::listAllUsers, "Listar Usuários"));
    }

    public void updateUserInfo(String username) {
        this.currentUsername = username;
        this.welcomeLabel.setText("Olá, " + username + "!");
        // Regra simples: se o usuário for "admin", habilita funções de admin
        this.isAdmin = "admin".equalsIgnoreCase(username);

        // Desabilita/Habilita aba de admin visualmente se necessário, ou apenas assume que o usuário
        // não vai clicar se não tiver permissão (o servidor bloqueia com 403 Forbidden).
        // Aqui deixamos visível, mas as requisições falhariam.
    }

    private void fetchAndShowMovies(boolean managementMode) {
        executeNetworkTask(() -> {
            JSONObject response = networkService.listAllMovies();
            SwingUtilities.invokeLater(() -> showElegantMovieList(response.optJSONArray("filmes"), managementMode));
            return response;
        }, "Carregar Filmes");
    }

    // =============================================================================================
    // UI: DETALHES DO FILME E REVIEWS
    // =============================================================================================

    /**
     * Busca os detalhes completos do filme (incluindo reviews) e abre o diálogo.
     */
    private void openMovieDetails(String movieId) {
        executeNetworkTask(() -> {
            JSONObject response = networkService.getMovieById(movieId);
            if ("200".equals(response.optString("status"))) {
                JSONObject movie = response.getJSONObject("filme");
                JSONArray reviews = response.optJSONArray("reviews");
                SwingUtilities.invokeLater(() -> showMovieDetailsDialog(movie, reviews));
            }
            return response;
        }, "Carregar Detalhes");
    }

    private void showMovieDetailsDialog(JSONObject movie, JSONArray reviews) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Detalhes do Filme", true);
        dialog.setSize(600, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // --- Cabeçalho: Dados do Filme ---
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        headerPanel.setBackground(new Color(30, 30, 30));

        JLabel titleLbl = new JLabel(movie.optString("titulo"));
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(NETFLIX_RED);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel metaLbl = new JLabel(String.format("%s • %s • %s",
                movie.optString("ano"), movie.optString("diretor"), movie.optString("nota", "N/A")));
        metaLbl.setForeground(Color.LIGHT_GRAY);
        metaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea sinopseArea = new JTextArea(movie.optString("sinopse"));
        sinopseArea.setLineWrap(true);
        sinopseArea.setWrapStyleWord(true);
        sinopseArea.setEditable(false);
        sinopseArea.setBackground(new Color(30, 30, 30));
        sinopseArea.setForeground(Color.WHITE);
        sinopseArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLbl);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(metaLbl);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(sinopseArea);

        // --- Lista de Reviews ---
        JPanel reviewsPanel = new JPanel();
        reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));

        if (reviews != null && reviews.length() > 0) {
            for (int i = 0; i < reviews.length(); i++) {
                reviewsPanel.add(createReviewCard(reviews.getJSONObject(i), dialog));
                reviewsPanel.add(Box.createVerticalStrut(10));
            }
        } else {
            reviewsPanel.add(new JLabel("Sem avaliações ainda. Seja o primeiro!"));
        }

        JScrollPane scrollPane = new JScrollPane(reviewsPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Avaliações da Comunidade"));

        // --- Botão de Adicionar Review ---
        JButton addReviewBtn = new JButton("Escrever Avaliação");
        styleButtonAsPrimary(addReviewBtn, true);
        addReviewBtn.addActionListener(e -> {
            showReviewForm(movie.optString("id"), null, dialog);
        });

        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(addReviewBtn, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createReviewCard(JSONObject review, JDialog parentDialog) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                new EmptyBorder(8, 8, 8, 8)
        ));

        String autor = review.optString("nome_usuario");
        String nota = review.optString("nota");
        String titulo = review.optString("titulo");
        String texto = review.optString("descricao");
        boolean isMyReview = autor.equals(currentUsername);

        JLabel header = new JLabel(String.format("<html><b>%s</b> deu nota <font color='orange'>%s</font> - <i>%s</i></html>", autor, nota, titulo));
        JTextArea body = new JTextArea(texto);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setOpaque(false);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        // Botões de Ação (Editar/Excluir)
        // Admin pode apagar qualquer um. Usuário pode apagar/editar o seu.
        if (isMyReview || isAdmin) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            if (isMyReview) {
                JButton editBtn = new JButton("Editar");
                editBtn.addActionListener(e -> {
                    showReviewForm(review.optString("id_filme"), review, parentDialog);
                });
                actions.add(editBtn);
            }

            JButton delBtn = new JButton("Apagar");
            styleButtonAsDestructive(delBtn);
            delBtn.addActionListener(e -> {
                if(JOptionPane.showConfirmDialog(card, "Apagar esta avaliação?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    executeNetworkTask(() -> networkService.deleteReview(review.getString("id")), "Apagar Review");
                    parentDialog.dispose(); // Fecha para forçar atualização
                }
            });
            actions.add(delBtn);

            card.add(actions, BorderLayout.SOUTH);
        }

        return card;
    }

    private void showReviewForm(String idFilme, JSONObject existingReview, JDialog parentDialog) {
        boolean isEdit = (existingReview != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Editar Avaliação" : "Nova Avaliação", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        JTextField tituloField = new JTextField(isEdit ? existingReview.optString("titulo") : "", 20);
        JTextField notaField = new JTextField(isEdit ? existingReview.optString("nota") : "", 5);
        JTextArea descArea = new JTextArea(isEdit ? existingReview.optString("descricao") : "", 5, 20);
        descArea.setLineWrap(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx=0; gbc.gridy=0; dialog.add(new JLabel("Título:"), gbc);
        gbc.gridx=1; dialog.add(tituloField, gbc);

        gbc.gridx=0; gbc.gridy=1; dialog.add(new JLabel("Nota (0-5):"), gbc);
        gbc.gridx=1; dialog.add(notaField, gbc);

        gbc.gridx=0; gbc.gridy=2; dialog.add(new JLabel("Opinião:"), gbc);
        gbc.gridx=1; dialog.add(new JScrollPane(descArea), gbc);

        JButton saveBtn = new JButton("Salvar");
        saveBtn.addActionListener(e -> {
            String t = tituloField.getText();
            String n = notaField.getText();
            String d = descArea.getText();

            dialog.dispose();

            if (isEdit) {
                String idReview = existingReview.optString("id");
                executeNetworkTask(() -> networkService.updateReview(idReview, t, d, n), "Editar Review");
            } else {
                executeNetworkTask(() -> networkService.createReview(idFilme, t, d, n), "Criar Review");
            }
            if(parentDialog != null) parentDialog.dispose(); // Refresh
        });

        gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2; dialog.add(saveBtn, gbc);
        dialog.setVisible(true);
    }

    // =============================================================================================
    // UI: LISTA DE FILMES
    // =============================================================================================

    private void showElegantMovieList(JSONArray movies, boolean managementMode) {
        if (movies == null) return;

        JDialog movieDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Catálogo VoteFlix", true);
        movieDialog.setSize(900, 600);
        movieDialog.setLocationRelativeTo(this);
        movieDialog.setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 10, 10)); // 3 colunas
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (movies.isEmpty()) {
            gridPanel.add(new JLabel("Nenhum filme encontrado."));
        } else {
            for (int i = 0; i < movies.length(); i++) {
                gridPanel.add(createMovieGridItem(movies.getJSONObject(i), managementMode, movieDialog));
            }
        }

        movieDialog.add(new JScrollPane(gridPanel), BorderLayout.CENTER);
        movieDialog.setVisible(true);
    }

    private JPanel createMovieGridItem(JSONObject movie, boolean managementMode, JDialog parentDialog) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        panel.setBackground(new Color(40, 40, 40));

        JLabel title = new JLabel(movie.optString("titulo"), SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);

        JLabel info = new JLabel("Nota: " + movie.optString("nota", "-"), SwingConstants.CENTER);
        info.setForeground(Color.LIGHT_GRAY);

        panel.add(title, BorderLayout.NORTH);
        panel.add(info, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 0));

        if (managementMode) {
            // Modo Admin: Editar / Excluir
            JButton editBtn = new JButton("Editar");
            JButton delBtn = new JButton("X");
            styleButtonAsDestructive(delBtn);

            editBtn.addActionListener(e -> {
                showMovieForm(movie.optString("id"), movie);
                parentDialog.dispose();
            });
            delBtn.addActionListener(e -> {
                if(JOptionPane.showConfirmDialog(panel, "Excluir filme?", "Confirmação", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                    executeNetworkTask(() -> networkService.deleteMovie(movie.optString("id")), "Excluir Filme");
                    parentDialog.dispose();
                }
            });
            btnPanel.add(editBtn);
            btnPanel.add(delBtn);
        } else {
            // Modo Usuário: Ver Detalhes
            JButton detailsBtn = new JButton("Ver Detalhes");
            styleButtonAsPrimary(detailsBtn, true);
            detailsBtn.addActionListener(e -> openMovieDetails(movie.optString("id")));
            btnPanel.add(detailsBtn);
        }

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // --- Auxiliares (Forms de Filme, etc. mantidos do original, mas adaptados) ---

    private void showMovieForm(String idToEdit, JSONObject existingData) {
        // (Código do formulário de filme permanece similar ao anterior,
        //  chamando createMovie ou updateMovie do NetworkService)
        // ... Implementação resumida para brevidade ...
        boolean isEditing = (idToEdit != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEditing ? "Editar Filme" : "Novo Filme", true);
        dialog.setLayout(new GridLayout(6, 2));
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JTextField tField = new JTextField(existingData != null ? existingData.optString("titulo") : "");
        JTextField dField = new JTextField(existingData != null ? existingData.optString("diretor") : "");
        JTextField aField = new JTextField(existingData != null ? existingData.optString("ano") : "");
        JTextField gField = new JTextField(); // Simplificado para exemplo
        JTextField sField = new JTextField(existingData != null ? existingData.optString("sinopse") : "");

        dialog.add(new JLabel("Título:")); dialog.add(tField);
        dialog.add(new JLabel("Diretor:")); dialog.add(dField);
        dialog.add(new JLabel("Ano:")); dialog.add(aField);
        dialog.add(new JLabel("Gêneros (sep. vírgula):")); dialog.add(gField);
        dialog.add(new JLabel("Sinopse:")); dialog.add(sField);

        JButton save = new JButton("Salvar");
        save.addActionListener(e -> {
            dialog.dispose();
            if(isEditing) executeNetworkTask(() -> networkService.updateMovie(idToEdit, tField.getText(), dField.getText(), aField.getText(), gField.getText(), sField.getText()), "Salvar Filme");
            else executeNetworkTask(() -> networkService.createMovie(tField.getText(), dField.getText(), aField.getText(), gField.getText(), sField.getText()), "Criar Filme");
        });
        dialog.add(save);
        dialog.setVisible(true);
    }

    private void showReviewsList(JSONArray reviews, String title) {
        // Janela simples para listar reviews (usado no botão "Minhas Avaliações")
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        d.setSize(500, 400);
        d.setLocationRelativeTo(this);
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        if(reviews != null) {
            for(int i=0; i<reviews.length(); i++) p.add(createReviewCard(reviews.getJSONObject(i), d));
        }
        d.add(new JScrollPane(p));
        d.setVisible(true);
    }

    // Helpers UI
    private JPanel createTabPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(20,20,20,20));
        return p;
    }
    private JButton createStyledButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }
    private void styleButtonAsPrimary(JButton b, boolean primary) {
        if(primary) { b.setBackground(NETFLIX_RED); b.setForeground(Color.WHITE); }
    }
    private void styleButtonAsDestructive(JButton b) {
        b.setBackground(Color.DARK_GRAY); b.setForeground(Color.RED);
    }

    private void executeNetworkTask(NetworkTask task, String title) {
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception { return task.execute(); }
            @Override protected void done() {
                try {
                    JSONObject res = get();
                    if(res == null) return;
                    String status = res.optString("status", "500");
                    String msg = res.optString("mensagem", "");
                    if(status.startsWith("2")) {
                        JOptionPane.showMessageDialog(DashboardPanel.this, "Sucesso!\n" + msg, title, JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(DashboardPanel.this, "Erro (" + status + "):\n" + msg, title, JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    @FunctionalInterface interface NetworkTask { JSONObject execute() throws Exception; }
}