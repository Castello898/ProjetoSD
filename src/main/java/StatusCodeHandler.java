import java.util.Map;
import java.util.HashMap;

/**
 * Classe utilitária para traduzir códigos de status do servidor
 * em mensagens amigáveis para o usuário, com base na tabela de erros fornecida.
 */
public class StatusCodeHandler {

    private static final Map<Integer, String> messages = new HashMap<>();

    // Bloco estático para popular o mapa com as mensagens da imagem
    static {
        // Sucesso (2xx)
        messages.put(200, "Operação realizada com sucesso."); // Mensagem genérica da imagem: "OK"
        messages.put(201, "A sua requisição foi bem-sucedida e, como resultado, um novo recurso foi criado no servidor.");

        // Erro Cliente (4xx)
        messages.put(400, "Requisição inválida: O id fornecido não é válido (ex: está vazio ou no formato incorreto).");
        messages.put(401, "Não autorizado: Credenciais inválidas.");
        messages.put(403, "Proibido: Você tentou acessar/excluir um recurso que não lhe pertence ou para o qual não tem privilégios de administrador.");
        messages.put(404, "Não encontrado: O id do filme/review/usuário não existe.");
        messages.put(409, "Conflito: A instância já existe."); // Conforme sua imagem (409 Already exists)
        messages.put(410, "Recurso indisponível: O recurso que você está tentando acessar existiu no passado, mas foi intencionalmente removido e não voltará.");
        messages.put(411, "Tamanho necessário: O servidor precisa saber o tamanho do conteúdo (Content-Length).");
        messages.put(413, "Conteúdo muito grande: A requisição é maior do que o servidor está disposto a processar.");
        messages.put(418, "Eu sou uma chaleira. O servidor se recusa a preparar café porque ele é, permanentemente, uma chaleira."); // Da imagem
        messages.put(422, "Entidade não processável: Dados faltantes ou fora do padrão.");

        // Erro Servidor (5xx)
        messages.put(500, "Erro Interno do Servidor: Uma falha inesperada no servidor ou no banco de dados impediu a operação.");
    }

    /**
     * Obtém a mensagem amigável correspondente a um código de status.
     *
     * @param statusCode O código de status (ex: 404, 500).
     * @return Uma string com a descrição amigável do erro.
     */
    public static String getMessage(int statusCode) {
        // Retorna a mensagem do mapa ou uma mensagem padrão se o código não for encontrado
        return messages.getOrDefault(statusCode, "Ocorreu um erro desconhecido (Código: " + statusCode + ")");
    }
}