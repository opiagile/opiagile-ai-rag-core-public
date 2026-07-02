package com.opiagile.supportai.tool;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenAiToolPlanProvider implements ToolPlanProvider {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final int maxOutputTokens;

    public OpenAiToolPlanProvider(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.chat-model:gpt-5-mini}") String model,
            @Value("${tools.planner.llm.enabled:true}") boolean enabled,
            @Value("${tools.planner.llm.max-output-tokens:120}") int maxOutputTokens,
            @Value("${openai.timeout-seconds:30}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.enabled = enabled;
        this.maxOutputTokens = Math.max(60, Math.min(maxOutputTokens, 300));
    }

    @Override
    public ToolPlanDecision decide(String currentMessage, List<ExternalToolRecord> availableTools) {
        if (!enabled || apiKey.isBlank() || currentMessage == null || currentMessage.isBlank()) {
            return ToolPlanDecision.none("Planner LLM desabilitado ou sem chave.");
        }
        if (!hasKnowledgeBaseTool(availableTools)) {
            return ToolPlanDecision.none("Ferramenta base-conhecimento-readonly não disponível.");
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(currentMessage))
                    .retrieve()
                    .body(JsonNode.class);
            return parseDecision(extractOutputText(response));
        } catch (RuntimeException exception) {
            return ToolPlanDecision.none("Falha no planner LLM: " + exception.getClass().getSimpleName());
        }
    }

    private boolean hasKnowledgeBaseTool(List<ExternalToolRecord> tools) {
        if (tools == null || tools.isEmpty()) {
            return false;
        }
        return tools.stream()
                .anyMatch(tool -> "base-conhecimento-readonly".equals(tool.slug())
                        && "ACTIVE".equals(tool.status())
                        && "SQL_READ_ONLY".equals(tool.type()));
    }

    private Map<String, Object> requestBody(String currentMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", instructions());
        body.put("input", currentMessage);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("text", Map.of("verbosity", "low"));
        return body;
    }

    private String instructions() {
        return """
                Você é um classificador de uso de ferramenta para o core RAG da Opiagile.
                Sua tarefa é decidir se a mensagem do usuário pede uma consulta operacional segura sobre a base de conhecimento do workspace.
                Responda apenas JSON válido, sem markdown e sem texto adicional.

                Ações permitidas:
                - DOCUMENT_COUNT: contar documentos/arquivos por status. Use também para perguntas sobre tamanho, volume ou dimensão da base/workspace.
                - DOCUMENT_LIST: listar documentos/arquivos indexados. Use também para perguntas sobre quais conteúdos estão disponíveis.
                - CHUNK_STATS: contar trechos/chunks/fragmentos por documento.
                - RECENT_RETRIEVALS: listar perguntas/consultas recentes registradas.
                - NONE: qualquer outra solicitação.

                Regras:
                - Não gere SQL.
                - Não invente ação fora da lista.
                - Use NONE para dúvidas de FAQ, atendimento, comercial, agendamento, LGPD, integrações, WhatsApp, Slack, Teams ou perguntas gerais.
                - Use uma ação somente quando a intenção operacional estiver clara.

                Formato obrigatório:
                {"action":"DOCUMENT_COUNT|DOCUMENT_LIST|CHUNK_STATS|RECENT_RETRIEVALS|NONE","confidence":0.0,"reason":"curto"}
                """;
    }

    private ToolPlanDecision parseDecision(String outputText) {
        try {
            JsonNode json = objectMapper.readTree(outputText);
            ToolPlanAction action = parseAction(json.path("action").asText("NONE"));
            double confidence = json.path("confidence").asDouble(0.0);
            String reason = json.path("reason").asText("");
            if (confidence < 0.65 || action == ToolPlanAction.NONE) {
                return ToolPlanDecision.none(reason.isBlank() ? "Planner LLM retornou NONE ou baixa confiança." : reason);
            }
            return new ToolPlanDecision(action, Math.min(confidence, 1.0), reason);
        } catch (Exception exception) {
            return ToolPlanDecision.none("Planner LLM retornou JSON inválido.");
        }
    }

    private ToolPlanAction parseAction(String value) {
        try {
            return ToolPlanAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            return ToolPlanAction.NONE;
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("Resposta vazia da OpenAI.");
        }
        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }
        JsonNode output = response.path("output");
        if (output.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    JsonNode text = part.path("text");
                    if (text.isTextual() && !text.asText().isBlank()) {
                        builder.append(text.asText());
                    }
                }
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        throw new IllegalStateException("Não foi possível extrair texto da resposta da OpenAI.");
    }
}
