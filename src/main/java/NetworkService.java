import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Classe responsável pela comunicação com o servidor.
 * ATUALIZADO: Inclui métodos para gerenciamento de Reviews e busca detalhada.
 */
public class NetworkService {

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String token;

    public void connect(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
    }

    private JSONObject sendRequest(JSONObject request) throws IOException {
        if (out == null || in == null || clientSocket.isClosed()) {
            throw new IOException("A conexão não está ativa.");
        }

        String requestString = request.toString();
        System.out.println("[CLIENTE->SERVIDOR] Enviando: " + requestString);
        out.println(requestString);

        String responseStr = in.readLine();
        if (responseStr == null) {
            throw new IOException("O servidor encerrou a conexão inesperadamente.");
        }
        System.out.println("[SERVIDOR->CLIENTE] Resposta: " + responseStr);

        try {
            return new JSONObject(responseStr);
        } catch (JSONException e) {
            return new JSONObject()
                    .put("status", "500")
                    .put("mensagem", "Resposta inválida (não-JSON) do servidor: " + responseStr);
        }
    }

    // --- MÉTODOS DE CONTA E USUÁRIO ---

    public JSONObject registerUser(String nome, String password) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "CRIAR_USUARIO");
        JSONObject userData = new JSONObject();
        userData.put("nome", nome);
        userData.put("senha", password);
        request.put("usuario", userData);
        return sendRequest(request);
    }

    public JSONObject loginUser(String login, String password) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LOGIN");
        request.put("usuario", login);
        request.put("senha", password);
        JSONObject response = sendRequest(request);
        if (response.has("token")) {
            this.token = response.getString("token");
        }
        return response;
    }

    public JSONObject viewProfile() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LISTAR_PROPRIO_USUARIO");
        request.put("token", this.token);
        return sendRequest(request);
    }

    public JSONObject updateUserPassword(String newPassword) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EDITAR_PROPRIO_USUARIO");
        request.put("token", this.token);
        JSONObject userData = new JSONObject();
        userData.put("senha", newPassword);
        request.put("usuario", userData);
        return sendRequest(request);
    }

    public JSONObject deleteUser() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EXCLUIR_PROPRIO_USUARIO");
        request.put("token", this.token);
        return sendRequest(request);
    }

    public JSONObject logoutUser() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LOGOUT");
        request.put("token", this.token);
        JSONObject response = sendRequest(request);
        this.token = null;
        closeConnection();
        return response;
    }

    // --- MÉTODOS DE FILMES ---

    public JSONObject listAllMovies() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LISTAR_FILMES");
        request.put("token", this.token);
        return sendRequest(request);
    }

    /**
     * [cite_start]Busca filme por ID para ver detalhes e reviews[cite: 14].
     */
    public JSONObject getMovieById(String idFilme) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "BUSCAR_FILME_ID");
        request.put("id_filme", idFilme);
        request.put("token", this.token);
        return sendRequest(request);
    }

    public JSONObject createMovie(String titulo, String diretor, String ano, String generos, String sinopse) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "CRIAR_FILME");
        request.put("token", this.token);

        JSONObject filmeData = new JSONObject();
        filmeData.put("titulo", titulo);
        filmeData.put("diretor", diretor);
        filmeData.put("ano", ano);
        filmeData.put("genero", new JSONArray(generos.split(",")));
        filmeData.put("sinopse", sinopse);

        request.put("filme", filmeData);
        return sendRequest(request);
    }

    public JSONObject updateMovie(String id, String titulo, String diretor, String ano, String generos, String sinopse) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EDITAR_FILME");
        request.put("token", this.token);

        JSONObject filmeData = new JSONObject();
        filmeData.put("id", id);
        filmeData.put("titulo", titulo);
        filmeData.put("diretor", diretor);
        filmeData.put("ano", ano);
        filmeData.put("genero", new JSONArray(generos.split(",")));
        filmeData.put("sinopse", sinopse);

        request.put("filme", filmeData);
        return sendRequest(request);
    }

    public JSONObject deleteMovie(String id) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EXCLUIR_FILME");
        request.put("token", this.token);
        request.put("id", id);
        return sendRequest(request);
    }

    // --- MÉTODOS DE REVIEWS (Novos) ---

    /**
     * [cite_start]Cria uma nova review para um filme[cite: 5].
     */
    public JSONObject createReview(String idFilme, String titulo, String descricao, String nota) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "CRIAR_REVIEW");
        request.put("token", this.token);

        JSONObject reviewData = new JSONObject();
        reviewData.put("id_filme", idFilme);
        reviewData.put("titulo", titulo);
        reviewData.put("descricao", descricao);
        reviewData.put("nota", nota);

        request.put("review", reviewData);
        return sendRequest(request);
    }

    /**
     * [cite_start]Edita uma review existente[cite: 23].
     */
    public JSONObject updateReview(String idReview, String titulo, String descricao, String nota) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EDITAR_REVIEW");
        request.put("token", this.token);

        JSONObject reviewData = new JSONObject();
        reviewData.put("id", idReview);
        reviewData.put("titulo", titulo);
        reviewData.put("descricao", descricao);
        reviewData.put("nota", nota);

        request.put("review", reviewData);
        return sendRequest(request);
    }

    /**
     * [cite_start]Exclui uma review (Própria ou por Admin)[cite: 29].
     */
    public JSONObject deleteReview(String idReview) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EXCLUIR_REVIEW");
        request.put("token", this.token);
        request.put("id", idReview);
        return sendRequest(request);
    }

    public JSONObject listMyReviews() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LISTAR_REVIEWS_USUARIO");
        request.put("token", this.token);
        return sendRequest(request);
    }

    // --- MÉTODOS DE ADMINISTRAÇÃO DE USUÁRIOS ---

    public JSONObject listAllUsers() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LISTAR_USUARIOS");
        request.put("token", this.token);
        return sendRequest(request);
    }

    public JSONObject deleteOtherUser(String id) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "ADMIN_EXCLUIR_USUARIO");
        request.put("token", this.token);
        request.put("id", id);
        return sendRequest(request);
    }

    public JSONObject updateOtherUserPassword(String id, String newPassword) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "ADMIN_EDITAR_USUARIO");
        request.put("token", this.token);
        request.put("id", id);

        JSONObject userData = new JSONObject();
        userData.put("senha", newPassword);
        request.put("usuario", userData);

        return sendRequest(request);
    }

    public void closeConnection() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }
}