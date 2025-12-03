import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardPanel extends JPanel {
    private final CardLayout rootCardLayout;
    private final JPanel mainContainer;
    private final NetworkService networkService;

    // Layout interno do Dashboard
    private JPanel contentArea;
    private CardLayout contentLayout;

    private String currentUsername;
    private boolean isAdmin;
    private JLabel userLabel;

    // Paineis internos
    private JPanel moviesPanel;
    private JPanel myReviewsPanel;
    private JPanel adminUsersPanel;
    private JPanel profilePanel;

    // Lista de Gêneros Padrão
    private static final String[] GENRES_LIST = {
            "Ação", "Aventura", "Comédia", "Drama", "Fantasia",
            "Ficção Científica", "Terror", "Romance", "Documentário",
            "Musical", "Animação"
    };

    public DashboardPanel(JPanel mainContainer, CardLayout rootCardLayout, NetworkService networkService) {
        this.mainContainer = mainContainer;
        this.rootCardLayout = rootCardLayout;
        this.networkService = networkService;

        setLayout(new BorderLayout());
        setBackground(StyleTheme.BG_COLOR);

        // 1. Sidebar (Menu Lateral)
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // 2. Área de Conteúdo (Direita)
        contentLayout = new CardLayout();
        contentArea = new JPanel(contentLayout);
        contentArea.setBackground(StyleTheme.BG_COLOR);
        contentArea.setBorder(new EmptyBorder(20, 20, 20, 20));

        add(contentArea, BorderLayout.CENTER);

        // Inicializa as "Views" vazias
        moviesPanel = new JPanel(new BorderLayout());
        myReviewsPanel = new JPanel(new BorderLayout());
        adminUsersPanel = new JPanel(new BorderLayout());
        profilePanel = new JPanel(new GridBagLayout());

        contentArea.add(moviesPanel, "MOVIES");
        contentArea.add(myReviewsPanel, "REVIEWS");
        contentArea.add(adminUsersPanel, "ADMIN_USERS");
        contentArea.add(profilePanel, "PROFILE");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(StyleTheme.PANEL_COLOR);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        // Logo / Título
        JLabel logo = new JLabel("VOTEFLIX");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(StyleTheme.ACCENT_COLOR);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        userLabel = new JLabel("Carregando...");
        userLabel.setFont(StyleTheme.FONT_REGULAR);
        userLabel.setForeground(Color.LIGHT_GRAY);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(userLabel);
        sidebar.add(Box.createVerticalStrut(30));

        // Botões de Navegação
        sidebar.add(createMenuButton("🎬 Catálogo de Filmes", e -> loadMoviesView()));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createMenuButton("⭐ Minhas Avaliações", e -> loadMyReviewsView()));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createMenuButton("👤 Meu Perfil", e -> loadProfileView()));

        sidebar.add(Box.createVerticalStrut(30));
        JLabel adminLbl = new JLabel("ADMINISTRAÇÃO");
        adminLbl.setForeground(Color.GRAY);
        adminLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(adminLbl);
        sidebar.add(Box.createVerticalStrut(10));

        JButton usersBtn = createMenuButton("👥 Gerenciar Usuários", e -> loadAdminUsersView());
        sidebar.add(usersBtn);

        sidebar.add(Box.createVerticalGlue());

        JButton logoutBtn = createMenuButton("🚪 Sair", e -> performLogout());
        logoutBtn.setBackground(new Color(60, 20, 20));
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private JButton createMenuButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(StyleTheme.FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(StyleTheme.PANEL_COLOR);
        btn.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(50, 50, 50)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(StyleTheme.PANEL_COLOR); }
        });

        btn.addActionListener(action);
        return btn;
    }

    public void updateUserInfo(String username) {
        this.currentUsername = username;
        this.isAdmin = "admin".equalsIgnoreCase(username);
        userLabel.setText(isAdmin ? username + " (Admin)" : username);
        loadMoviesView();
    }

    // =============================================================================================
    // VIEW: FILMES (Grid)
    // =============================================================================================
    private void loadMoviesView() {
        moviesPanel.removeAll();
        moviesPanel.setBackground(StyleTheme.BG_COLOR);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(StyleTheme.BG_COLOR);
        JLabel title = new JLabel("Filmes Disponíveis");
        title.setFont(StyleTheme.FONT_TITLE);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        if (isAdmin) {
            JButton addMovieBtn = StyleTheme.createButton("+ Novo Filme", true);
            addMovieBtn.addActionListener(e -> showMovieForm(null, null));
            header.add(addMovieBtn, BorderLayout.EAST);
        }
        moviesPanel.add(header, BorderLayout.NORTH);

        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return networkService.listAllMovies();
            }
            @Override protected void done() {
                try {
                    JSONObject res = get();
                    JSONArray movies = res.optJSONArray("filmes");
                    JPanel grid = new JPanel(new GridLayout(0, 3, 15, 15));
                    grid.setBackground(StyleTheme.BG_COLOR);

                    if (movies != null) {
                        for (int i = 0; i < movies.length(); i++) {
                            grid.add(createMovieCard(movies.getJSONObject(i)));
                        }
                    }

                    JScrollPane scroll = new JScrollPane(grid);
                    scroll.setBorder(null);
                    scroll.getVerticalScrollBar().setUnitIncrement(16);
                    moviesPanel.add(scroll, BorderLayout.CENTER);

                    contentLayout.show(contentArea, "MOVIES");
                    moviesPanel.revalidate();
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    private JPanel createMovieCard(JSONObject movie) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(StyleTheme.PANEL_COLOR);
        card.setBorder(BorderFactory.createLineBorder(new Color(60,60,60)));
        card.setMaximumSize(new Dimension(200, 250));

        JLabel title = new JLabel(movie.optString("titulo"));
        title.setFont(StyleTheme.FONT_BOLD);
        title.setForeground(StyleTheme.ACCENT_COLOR);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info = new JLabel(movie.optString("ano") + " | " + movie.optString("diretor"));
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(Color.GRAY);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel rating = new JLabel("★ " + movie.optString("nota", "-"));
        rating.setForeground(Color.YELLOW);
        rating.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(StyleTheme.PANEL_COLOR);

        JButton viewBtn = new JButton("Ver");
        viewBtn.setBackground(StyleTheme.INPUT_BG);
        viewBtn.setForeground(Color.WHITE);
        viewBtn.addActionListener(e -> openMovieDetails(movie.optString("id")));
        btnPanel.add(viewBtn);

        if (isAdmin) {
            JButton editBtn = new JButton("✎");
            editBtn.setToolTipText("Editar Filme");
            editBtn.addActionListener(e -> showMovieForm(movie.optString("id"), movie));

            JButton delBtn = new JButton("✖");
            delBtn.setForeground(Color.RED);
            delBtn.setToolTipText("Excluir Filme");
            delBtn.addActionListener(e -> {
                if (confirmAction("Excluir filme " + movie.optString("titulo") + "?")) {
                    executeTask(() -> networkService.deleteMovie(movie.optString("id")), "Excluir", this::loadMoviesView);
                }
            });
            btnPanel.add(editBtn);
            btnPanel.add(delBtn);
        }

        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(info);
        card.add(rating);
        card.add(Box.createVerticalGlue());
        card.add(btnPanel);

        return card;
    }

    // =============================================================================================
    // VIEW: MINHAS REVIEWS
    // =============================================================================================
    private void loadMyReviewsView() {
        myReviewsPanel.removeAll();
        myReviewsPanel.setBackground(StyleTheme.BG_COLOR);

        JLabel title = new JLabel("Minhas Avaliações");
        title.setFont(StyleTheme.FONT_TITLE);
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0,0,20,0));
        myReviewsPanel.add(title, BorderLayout.NORTH);

        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return networkService.listMyReviews();
            }
            @Override protected void done() {
                try {
                    JSONArray reviews = get().optJSONArray("reviews");
                    JPanel listContainer = new JPanel();
                    listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
                    listContainer.setBackground(StyleTheme.BG_COLOR);

                    if (reviews != null && reviews.length() > 0) {
                        for(int i=0; i<reviews.length(); i++) {
                            listContainer.add(createReviewItem(reviews.getJSONObject(i), true));
                            listContainer.add(Box.createVerticalStrut(10));
                        }
                    } else {
                        JLabel empty = new JLabel("Você ainda não avaliou nenhum filme.");
                        empty.setForeground(Color.GRAY);
                        listContainer.add(empty);
                    }
                    myReviewsPanel.add(new JScrollPane(listContainer), BorderLayout.CENTER);
                    contentLayout.show(contentArea, "REVIEWS");
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    /**
     * ATUALIZADO: Agora verifica se a review foi editada e exibe o label.
     */
    private JPanel createReviewItem(JSONObject review, boolean allowEdit) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(StyleTheme.PANEL_COLOR);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,60,60)),
                new EmptyBorder(10,10,10,10)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        // Verifica se foi editado (o servidor retorna "true" ou "false" como string ou boolean)
        boolean isEdited = "true".equalsIgnoreCase(review.optString("editado"));
        String editedLabel = isEdited ? " <font color='#999' size='-2'>(Editado)</font>" : "";

        String headerHtml = String.format("<html><b>%s</b> <font color='yellow'>★ %s</font>%s</html>",
                review.optString("titulo"), review.optString("nota"), editedLabel);

        JLabel header = new JLabel(headerHtml);
        header.setForeground(Color.WHITE);

        JTextArea desc = new JTextArea(review.optString("descricao"));
        desc.setLineWrap(true);
        desc.setEditable(false);
        desc.setBackground(StyleTheme.PANEL_COLOR);
        desc.setForeground(Color.LIGHT_GRAY);

        p.add(header, BorderLayout.NORTH);
        p.add(desc, BorderLayout.CENTER);

        if (allowEdit || isAdmin) {
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actions.setBackground(StyleTheme.PANEL_COLOR);

            if (allowEdit) {
                JButton edit = new JButton("Editar");
                edit.addActionListener(e -> showReviewForm(review.optString("id_filme"), review, null));
                actions.add(edit);
            }

            JButton del = new JButton("Excluir");
            del.setForeground(Color.RED);
            del.addActionListener(e -> {
                if(confirmAction("Apagar esta avaliação?")) {
                    executeTask(() -> networkService.deleteReview(review.getString("id")), "Apagar Review",
                            allowEdit ? this::loadMyReviewsView : null);
                }
            });
            actions.add(del);
            p.add(actions, BorderLayout.SOUTH);
        }
        return p;
    }

    // =============================================================================================
    // VIEW: ADMIN USERS
    // =============================================================================================
    private void loadAdminUsersView() {
        if(!isAdmin) {
            JOptionPane.showMessageDialog(this, "Acesso Negado.");
            return;
        }

        adminUsersPanel.removeAll();
        adminUsersPanel.setBackground(StyleTheme.BG_COLOR);

        JLabel title = new JLabel("Gestão de Usuários");
        title.setFont(StyleTheme.FONT_TITLE);
        title.setForeground(Color.WHITE);
        adminUsersPanel.add(title, BorderLayout.NORTH);

        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return networkService.listAllUsers();
            }
            @Override protected void done() {
                try {
                    JSONArray users = get().optJSONArray("usuarios");
                    String[] cols = {"ID", "Nome"};
                    DefaultTableModel model = new DefaultTableModel(cols, 0) {
                        public boolean isCellEditable(int row, int col) { return false; }
                    };

                    if(users != null) {
                        for(int i=0; i<users.length(); i++) {
                            JSONObject u = users.getJSONObject(i);
                            model.addRow(new Object[]{ u.getString("id"), u.getString("nome") });
                        }
                    }

                    JTable table = new JTable(model);
                    table.setBackground(StyleTheme.PANEL_COLOR);
                    table.setForeground(Color.WHITE);
                    table.setRowHeight(25);

                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem editItem = new JMenuItem("Alterar Senha");
                    JMenuItem delItem = new JMenuItem("Excluir Usuário");
                    popup.add(editItem);
                    popup.add(delItem);

                    table.setComponentPopupMenu(popup);

                    table.addMouseListener(new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                            int r = table.rowAtPoint(e.getPoint());
                            if (r >= 0 && r < table.getRowCount()) {
                                table.setRowSelectionInterval(r, r);
                            } else {
                                table.clearSelection();
                            }
                        }
                    });

                    editItem.addActionListener(e -> {
                        int row = table.getSelectedRow();
                        if(row == -1) return;
                        String id = (String) model.getValueAt(row, 0);
                        String nome = (String) model.getValueAt(row, 1);
                        String newPass = JOptionPane.showInputDialog(DashboardPanel.this, "Nova senha para " + nome + ":");
                        if(newPass != null && !newPass.trim().isEmpty()) {
                            executeTask(() -> networkService.updateOtherUserPassword(id, newPass), "Alterar Senha", null);
                        }
                    });

                    delItem.addActionListener(e -> {
                        int row = table.getSelectedRow();
                        if(row == -1) return;
                        String id = (String) model.getValueAt(row, 0);
                        String nome = (String) model.getValueAt(row, 1);
                        if(confirmAction("Excluir usuário " + nome + "?\nIsso apagará todas as reviews dele.")) {
                            executeTask(() -> networkService.deleteOtherUser(id), "Excluir Usuário", DashboardPanel.this::loadAdminUsersView);
                        }
                    });

                    adminUsersPanel.add(new JScrollPane(table), BorderLayout.CENTER);
                    contentLayout.show(contentArea, "ADMIN_USERS");
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    // =============================================================================================
    // VIEW: PROFILE
    // =============================================================================================
    private void loadProfileView() {
        profilePanel.removeAll();
        profilePanel.setBackground(StyleTheme.BG_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel title = new JLabel("Meu Perfil");
        title.setFont(StyleTheme.FONT_TITLE);
        title.setForeground(Color.WHITE);
        profilePanel.add(title, gbc);

        gbc.gridy++;
        JLabel info = new JLabel("Usuário: " + currentUsername + (isAdmin ? " [ADMINISTRADOR]" : ""));
        info.setForeground(Color.LIGHT_GRAY);
        profilePanel.add(info, gbc);

        gbc.gridy++;
        JButton changePass = StyleTheme.createButton("Alterar Minha Senha", false);
        changePass.addActionListener(e -> {
            String newPass = JOptionPane.showInputDialog(this, "Nova senha:");
            if (newPass != null && !newPass.trim().isEmpty()) {
                executeTask(() -> networkService.updateUserPassword(newPass), "Senha", null);
            }
        });
        profilePanel.add(changePass, gbc);

        gbc.gridy++;
        JButton deleteAcc = new JButton("Excluir Minha Conta");
        deleteAcc.setForeground(Color.RED);
        deleteAcc.setBackground(new Color(40, 20, 20));
        deleteAcc.addActionListener(e -> {
            if(confirmAction("TEM CERTEZA? Essa ação é irreversível.")) {
                executeTask(() -> {
                    JSONObject r = networkService.deleteUser();
                    if("200".equals(r.optString("status"))) SwingUtilities.invokeLater(() -> rootCardLayout.show(mainContainer, "CONNECTION"));
                    return r;
                }, "Excluir Conta", null);
            }
        });
        profilePanel.add(deleteAcc, gbc);

        contentLayout.show(contentArea, "PROFILE");
    }

    // =============================================================================================
    // HELPERS & FORMS
    // =============================================================================================

    private void openMovieDetails(String movieId) {
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception {
                return networkService.getMovieById(movieId);
            }
            @Override protected void done() {
                try {
                    JSONObject res = get();
                    if(!"200".equals(res.optString("status"))) return;

                    JSONObject movie = res.getJSONObject("filme");
                    JSONArray reviews = res.optJSONArray("reviews");

                    JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(DashboardPanel.this), movie.getString("titulo"), true);
                    d.setSize(600, 700);
                    d.setLocationRelativeTo(DashboardPanel.this);

                    JPanel main = new JPanel(new BorderLayout());
                    main.setBackground(StyleTheme.BG_COLOR);

                    // Info Panel
                    JPanel info = new JPanel();
                    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                    info.setBackground(StyleTheme.BG_COLOR);
                    info.setBorder(new EmptyBorder(20,20,20,20));

                    JLabel title = new JLabel(movie.getString("titulo"));
                    title.setFont(StyleTheme.FONT_TITLE);
                    title.setForeground(StyleTheme.ACCENT_COLOR);

                    // Exibir Gêneros formatados
                    JSONArray genArr = movie.optJSONArray("genero");
                    String genStr = (genArr != null) ? genArr.join(", ").replace("\"", "") : "";
                    JLabel genres = new JLabel(genStr);
                    genres.setForeground(Color.GRAY);

                    JTextArea sinopse = new JTextArea(movie.optString("sinopse"));
                    sinopse.setLineWrap(true);
                    sinopse.setWrapStyleWord(true);
                    sinopse.setEditable(false);
                    sinopse.setBackground(StyleTheme.BG_COLOR);
                    sinopse.setForeground(Color.LIGHT_GRAY);

                    info.add(title);
                    info.add(genres);
                    info.add(new JLabel(" "));
                    info.add(sinopse);

                    // Reviews List
                    JPanel reviewsList = new JPanel();
                    reviewsList.setLayout(new BoxLayout(reviewsList, BoxLayout.Y_AXIS));
                    reviewsList.setBackground(StyleTheme.PANEL_COLOR);

                    if(reviews != null) {
                        for(int i=0; i<reviews.length(); i++) {
                            reviewsList.add(createReviewItem(reviews.getJSONObject(i), false));
                            reviewsList.add(Box.createVerticalStrut(10));
                        }
                    }

                    JButton addRev = StyleTheme.createButton("Escrever Avaliação", true);
                    addRev.addActionListener(e -> {
                        d.dispose();
                        showReviewForm(movieId, null, () -> openMovieDetails(movieId));
                    });

                    main.add(info, BorderLayout.NORTH);
                    main.add(new JScrollPane(reviewsList), BorderLayout.CENTER);
                    main.add(addRev, BorderLayout.SOUTH);

                    d.add(main);
                    d.setVisible(true);

                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    private void showReviewForm(String idFilme, JSONObject existing, Runnable onSuccess) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Avaliação", true);
        d.setSize(400, 350);
        d.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridLayout(6,1, 10, 10));
        p.setBorder(new EmptyBorder(20,20,20,20));
        p.setBackground(StyleTheme.BG_COLOR);

        JTextField titleF = StyleTheme.createTextField();
        JTextField notaF = StyleTheme.createTextField();
        JTextArea descF = new JTextArea(5, 20); descF.setBackground(StyleTheme.INPUT_BG); descF.setForeground(Color.WHITE);

        if(existing != null) {
            titleF.setText(existing.optString("titulo"));
            notaF.setText(existing.optString("nota"));
            descF.setText(existing.optString("descricao"));
        }

        p.add(new JLabel("Título:") {{ setForeground(Color.WHITE); }}); p.add(titleF);
        p.add(new JLabel("Nota (0-5):") {{ setForeground(Color.WHITE); }}); p.add(notaF);
        p.add(new JLabel("Opinião:") {{ setForeground(Color.WHITE); }}); p.add(new JScrollPane(descF));

        JButton save = StyleTheme.createButton("Publicar", true);
        save.addActionListener(e -> {
            executeTask(() -> {
                if(existing == null) return networkService.createReview(idFilme, titleF.getText(), descF.getText(), notaF.getText());
                else return networkService.updateReview(existing.getString("id"), titleF.getText(), descF.getText(), notaF.getText());
            }, "Review", () -> {
                d.dispose();
                if(onSuccess != null) onSuccess.run();
                else loadMyReviewsView();
            });
        });

        p.add(save);
        d.add(p);
        d.setVisible(true);
    }

    /**
     * ATUALIZADO: Substituiu o campo de texto de gêneros por CheckBoxes.
     */
    private void showMovieForm(String id, JSONObject data) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Filme", true);
        d.setSize(500, 600); // Aumentei um pouco a altura para caber os checkboxes
        d.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(StyleTheme.BG_COLOR);
        p.setBorder(new EmptyBorder(10,10,10,10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField tit = StyleTheme.createTextField();
        JTextField dir = StyleTheme.createTextField();
        JTextField ano = StyleTheme.createTextField();
        JTextArea sin = new JTextArea(3, 20); sin.setBackground(StyleTheme.INPUT_BG); sin.setForeground(Color.WHITE);

        // Painel de Checkboxes para Gêneros
        JPanel genresPanel = new JPanel(new GridLayout(0, 2)); // 2 colunas de checkboxes
        genresPanel.setBackground(StyleTheme.BG_COLOR);
        List<JCheckBox> genreChecks = new ArrayList<>();

        // Pega os gêneros existentes se estiver editando
        List<String> currentGenres = new ArrayList<>();
        if (data != null && data.has("genero")) {
            JSONArray arr = data.getJSONArray("genero");
            for(int i=0; i<arr.length(); i++) currentGenres.add(arr.getString(i));
        }

        for (String genre : GENRES_LIST) {
            JCheckBox cb = new JCheckBox(genre);
            cb.setBackground(StyleTheme.BG_COLOR);
            cb.setForeground(Color.WHITE);
            cb.setFocusPainted(false);
            if (currentGenres.contains(genre)) {
                cb.setSelected(true);
            }
            genreChecks.add(cb);
            genresPanel.add(cb);
        }

        if(data != null) {
            tit.setText(data.optString("titulo"));
            dir.setText(data.optString("diretor"));
            ano.setText(data.optString("ano"));
            sin.setText(data.optString("sinopse"));
        }

        // Montando o Form
        gbc.gridx=0; gbc.gridy=0; p.add(new JLabel("Título") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridx=1; p.add(tit, gbc);

        gbc.gridx=0; gbc.gridy=1; p.add(new JLabel("Diretor") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridx=1; p.add(dir, gbc);

        gbc.gridx=0; gbc.gridy=2; p.add(new JLabel("Ano") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridx=1; p.add(ano, gbc);

        gbc.gridx=0; gbc.gridy=3; p.add(new JLabel("Sinopse") {{ setForeground(Color.WHITE); }}, gbc);
        gbc.gridx=1; p.add(new JScrollPane(sin), gbc);

        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2;
        p.add(new JLabel("Selecione os Gêneros:") {{ setForeground(StyleTheme.ACCENT_COLOR); }}, gbc);

        gbc.gridy=5;
        p.add(genresPanel, gbc);

        JButton save = StyleTheme.createButton("Salvar", true);
        save.addActionListener(e -> {
            // Coleta os gêneros marcados
            String selectedGenres = genreChecks.stream()
                    .filter(JCheckBox::isSelected)
                    .map(JCheckBox::getText)
                    .collect(Collectors.joining(","));

            if (selectedGenres.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Selecione pelo menos um gênero.");
                return;
            }

            executeTask(() -> {
                if(id == null) return networkService.createMovie(tit.getText(), dir.getText(), ano.getText(), selectedGenres, sin.getText());
                else return networkService.updateMovie(id, tit.getText(), dir.getText(), ano.getText(), selectedGenres, sin.getText());
            }, "Filme", () -> { d.dispose(); loadMoviesView(); });
        });

        gbc.gridy=6;
        p.add(save, gbc);

        d.add(p);
        d.setVisible(true);
    }

    private void performLogout() {
        executeTask(networkService::logoutUser, "Logout", () -> rootCardLayout.show(mainContainer, "CONNECTION"));
    }

    private boolean confirmAction(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void executeTask(NetworkTask task, String name, Runnable onSuccess) {
        new SwingWorker<JSONObject, Void>() {
            @Override protected JSONObject doInBackground() throws Exception { return task.run(); }
            @Override protected void done() {
                try {
                    JSONObject res = get();
                    String status = res.optString("status");
                    if(status.startsWith("2")) {
                        if(onSuccess != null) onSuccess.run();
                        else JOptionPane.showMessageDialog(DashboardPanel.this, res.optString("mensagem"));
                    } else {
                        JOptionPane.showMessageDialog(DashboardPanel.this, "Erro: " + res.optString("mensagem"));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    @FunctionalInterface interface NetworkTask { JSONObject run() throws Exception; }
}