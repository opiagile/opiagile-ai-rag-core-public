package com.opiagile.supportai.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Component
public class OpenAiChatModelProvider {

    private final RestClient restClient;
    private final RagAnswerPromptBuilder promptBuilder;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;
    private final String reasoningEffort;
    private final String textVerbosity;

    public OpenAiChatModelProvider(
            RestClient.Builder restClientBuilder,
            RagAnswerPromptBuilder promptBuilder,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.chat-model:gpt-5-mini}") String model,
            @Value("${openai.max-output-tokens:600}") int maxOutputTokens,
            @Value("${openai.timeout-seconds:30}") int timeoutSeconds,
            @Value("${openai.reasoning-effort:minimal}") String reasoningEffort,
            @Value("${openai.text-verbosity:low}") String textVerbosity) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
        this.promptBuilder = promptBuilder;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.maxOutputTokens = Math.max(120, maxOutputTokens);
        this.reasoningEffort = normalize(reasoningEffort, "minimal");
        this.textVerbosity = normalize(textVerbosity, "low");
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public ChatGenerationResult generate(ChatPrompt prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("OPENAI_API_KEY não configurada.");
        }
        Instant startedAt = Instant.now();
        JsonNode response = restClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(prompt))
                .retrieve()
                .body(JsonNode.class);

        String answer = extractOutputText(response);
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        JsonNode usage = response == null ? null : response.path("usage");
        return new ChatGenerationResult(
                answer,
                "LLM",
                "OPENAI",
                model,
                null,
                latencyMs,
                intOrNull(usage, "input_tokens"),
                intOrNull(usage, "output_tokens"),
                intOrNull(usage, "total_tokens"));
    }

    public String model() {
        return model;
    }

    private Map<String, Object> requestBody(ChatPrompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", promptBuilder.instructions());
        body.put("input", promptBuilder.input(prompt));
        body.put("max_output_tokens", maxOutputTokens);
        body.put("reasoning", Map.of("effort", reasoningEffort));
        body.put("text", Map.of("verbosity", textVerbosity));
        return body;
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
                        if (!builder.isEmpty()) {
                            builder.append("\n");
                        }
                        builder.append(text.asText());
                    }
                }
            }
            if (!builder.isEmpty()) {
                return builder.toString();
            }
        }
        throw new IllegalStateException("Não foi possível extrair texto da resposta da OpenAI. status="
                + response.path("status").asText("desconhecido")
                + ", incompleteReason=" + response.path("incomplete_details").path("reason").asText("nenhum"));
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase();
    }

    private Integer intOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asInt();
    }
}
