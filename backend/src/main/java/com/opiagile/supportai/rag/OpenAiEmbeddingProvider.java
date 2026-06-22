package com.opiagile.supportai.rag;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(name = "openai.embeddings.enabled", havingValue = "true")
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int dimensions;

    public OpenAiEmbeddingProvider(
            RestClient.Builder restClientBuilder,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.embedding-model:text-embedding-3-small}") String model,
            @Value("${openai.embedding-dimensions:1536}") int dimensions,
            @Value("${openai.timeout-seconds:30}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "text-embedding-3-small" : model.trim();
        this.dimensions = Math.max(1, dimensions);
    }

    @Override
    public Optional<float[]> embed(String text) {
        if (!isConfigured() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(text))
                    .retrieve()
                    .body(JsonNode.class);
            return extractEmbedding(response);
        } catch (RestClientException | IllegalStateException exception) {
            return Optional.empty();
        }
    }

    @Override
    public String providerName() {
        return isConfigured() ? "openai" : "openai-sem-chave";
    }

    private boolean isConfigured() {
        return !apiKey.isBlank();
    }

    private Map<String, Object> requestBody(String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", text);
        body.put("dimensions", dimensions);
        return body;
    }

    private Optional<float[]> extractEmbedding(JsonNode response) {
        if (response == null) {
            return Optional.empty();
        }
        JsonNode embedding = response.path("data").path(0).path("embedding");
        if (!embedding.isArray() || embedding.isEmpty()) {
            return Optional.empty();
        }
        float[] values = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            values[i] = (float) embedding.get(i).asDouble();
        }
        return Optional.of(values);
    }
}
