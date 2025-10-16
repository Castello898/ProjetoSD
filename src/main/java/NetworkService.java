import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Classe responsável pela comunicação com o servidor.
 * VERSÃO CORRIGIDA de acordo com o arquivo "Protocolo de Troca de Mensagens.xlsx - Requisições.csv".
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
                    .put("status", 500);
                    //.put("mensagem", "Resposta inválida (não-JSON) do servidor: " + responseStr);
        }
    }

    /**
     * Cadastro de usuário (Operação: CRIAR_USUARIO)
     * CORREÇÃO: Os dados do usuário devem ser enviados dentro de um objeto JSON aninhado.
     */
    public JSONObject registerUser(String nome, String password) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "CRIAR_USUARIO");

        // CORREÇÃO: O protocolo exige um objeto "usuario" aninhado.
        JSONObject userData = new JSONObject();
        userData.put("nome", nome);
        userData.put("senha", password);

        request.put("usuario", userData);
        return sendRequest(request);
    }

    /**
     * Login de usuário (Operação: LOGIN)
     * CORREÇÃO: A chave para o nome de usuário é "usuario" e o token de resposta não está aninhado.
     */
    public JSONObject loginUser(String login, String password) throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LOGIN");
        // CORREÇÃO: A chave para o nome de usuário é "usuario", não "login".
        request.put("usuario", login);
        request.put("senha", password);

        JSONObject response = sendRequest(request);

        // CORREÇÃO: O token está no corpo principal da resposta.
        if (response.has("token")) {
            this.token = response.getString("token");
        }
        return response;
    }

    /**
     * Leitura dos dados do próprio usuário (Operação: LISTAR_PROPRIO_USUARIO)
     * CORREÇÃO: O nome da operação foi ajustado.
     */
    public JSONObject viewProfile() throws IOException {
        JSONObject request = new JSONObject();
        // CORREÇÃO: Nome da operação conforme o protocolo.
        request.put("operacao", "LISTAR_PROPRIO_USUARIO");
        request.put("token", this.token);
        return sendRequest(request);
    }

    /**
     * Atualização da senha do próprio usuário (Operação: EDITAR_PROPRIO_USUARIO)
     * CORREÇÃO: Nome da operação e estrutura da requisição ajustados.
     */
    public JSONObject updateUserPassword(String newPassword) throws IOException {
        JSONObject request = new JSONObject();
        // CORREÇÃO: Nome da operação conforme o protocolo.
        request.put("operacao", "EDITAR_PROPRIO_USUARIO");
        request.put("token", this.token);

        // CORREÇÃO: A nova senha deve ser enviada dentro de um objeto "usuario" aninhado.
        JSONObject userData = new JSONObject();
        userData.put("senha", newPassword);
        request.put("usuario", userData);

        return sendRequest(request);
    }

    /**
     * Apagar o próprio cadastro (Operação: EXCLUIR_PROPRIO_USUARIO)
     * CORREÇÃO: O nome da operação foi ajustado.
     */
    public JSONObject deleteUser() throws IOException {
        JSONObject request = new JSONObject();
        // CORREÇÃO: Nome da operação conforme o protocolo.
        request.put("operacao", "EXCLUIR_PROPRIO_USUARIO");
        request.put("token", this.token);
        return sendRequest(request);
    }

    /**
     * Logout (Operação: LOGOUT)
     * Nenhuma correção necessária, já estava conforme o protocolo.
     */
    public JSONObject logoutUser() throws IOException {
        JSONObject request = new JSONObject();
        request.put("operacao", "LOGOUT");
        request.put("token", this.token);

        JSONObject response = sendRequest(request);

        // Limpa o token local e fecha a conexão após o logout
        this.token = null;
        closeConnection();
        return response;
    }

    public void closeConnection() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
    }
}