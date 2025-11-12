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
 * VERSÃO ATUALIZADA COM MÉTODOS DE FILMES E ADMIN.
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

    // --- MÉTODOS DE USUÁRIO (Já existentes) ---

    public JSONObject registerUser(String nome, String password) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "CRIAR_USUARIO"); //
        JSONObject userData = new JSONObject();
        userData.put("nome", nome);
        userData.put("senha", password);
        request.put("usuario", userData);
        return sendRequest(request);
    }

    public JSONObject loginUser(String login, String password) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LOGIN"); //
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
        request.put("operacao", "LISTAR_PROPRIO_USUARIO"); //
        request.put("token", this.token);
        return sendRequest(request);
    }

    public JSONObject updateUserPassword(String newPassword) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EDITAR_PROPRIO_USUARIO"); //
        request.put("token", this.token);
        JSONObject userData = new JSONObject();
        userData.put("senha", newPassword);
        request.put("usuario", userData);
        return sendRequest(request);
    }

    public JSONObject deleteUser() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EXCLUIR_PROPRIO_USUARIO"); //
        request.put("token", this.token);
        return sendRequest(request);
    }

    public JSONObject logoutUser() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LOGOUT"); //
        request.put("token", this.token);
        JSONObject response = sendRequest(request);
        this.token = null;
        closeConnection();
        return response;
    }

    // --- NOVOS MÉTODOS (Itens Avaliados) ---

    /**
     * Item (c, g): Listar todos os filmes (ADM e Comum)
     */
    public JSONObject listAllMovies() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LISTAR_FILMES"); //
        request.put("token", this.token);
        return sendRequest(request);
    }

    /**
     * Item (f): Listar todos os usuários (ADM)
     */
    public JSONObject listAllUsers() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LISTAR_USUARIOS"); //
        request.put("token", this.token);
        return sendRequest(request);
    }

    /**
     * Item (b): Criar dados de filme (ADM)
     */
    public JSONObject createMovie(String titulo, String diretor, String ano, String generos, String sinopse) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "CRIAR_FILME"); // [cite: 275]
        request.put("token", this.token);

        JSONObject filmeData = new JSONObject();
        filmeData.put("titulo", titulo); // [cite: 234]
        filmeData.put("diretor", diretor); // [cite: 236]
        filmeData.put("ano", ano); // [cite: 235]
        // Converte a string "A,B,C" para um JSONArray ["A", "B", "C"] [cite: 240]
        filmeData.put("genero", new JSONArray(generos.split(",")));
        filmeData.put("sinopse", sinopse); // [cite: 238]

        request.put("filme", filmeData);
        return sendRequest(request);
    }

    /**
     * Item (d): Editar dados de filme (ADM)
     */
    public JSONObject updateMovie(String id, String titulo, String diretor, String ano, String generos, String sinopse) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EDITAR_FILME"); //
        request.put("token", this.token);

        JSONObject filmeData = new JSONObject();
        filmeData.put("id", id); // [cite: 233]
        filmeData.put("titulo", titulo);
        filmeData.put("diretor", diretor);
        filmeData.put("ano", ano);
        filmeData.put("genero", new JSONArray(generos.split(",")));
        filmeData.put("sinopse", sinopse);

        request.put("filme", filmeData);
        return sendRequest(request);
    }

    /**
     * Item (e): Apagar dados de filmes (ADM)
     */
    public JSONObject deleteMovie(String id) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "EXCLUIR_FILME"); //
        request.put("token", this.token);
        request.put("id", id); // ID é enviado fora do objeto 'filme'
        return sendRequest(request);
    }

    // --- FIM DOS NOVOS MÉTODOS ---

    public void closeConnection() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }
}