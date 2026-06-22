package com.opiagile.supportai.rag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class RetrievalLogRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public RetrievalLogRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void save(
            TenantContext tenantContext,
            UUID conversationId,
            String query,
            List<RetrievedChunk> chunks,
            int latencyMs,
            String intent,
            boolean handoffRequired,
            String fallbackReason,
            String provider,
            String responseMode,
            String llmProvider,
            String model,
            Integer llmLatencyMs,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens) {
        jdbc.sql("""
                INSERT INTO retrieval_logs (
                    tenant_id, workspace_id, conversation_id, query, retrieved_chunks, latency_ms, intent, handoff_required, fallback_reason,
                    provider, response_mode, llm_provider, model, llm_latency_ms, prompt_tokens, completion_tokens, total_tokens
                )
                VALUES (
                    :tenantId, :workspaceId, :conversationId, :query, CAST(:retrievedChunks AS jsonb), :latencyMs, :intent, :handoffRequired, :fallbackReason,
                    :provider, :responseMode, :llmProvider, :model, :llmLatencyMs, :promptTokens, :completionTokens, :totalTokens
                )
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("conversationId", conversationId)
                .param("query", query)
                .param("retrievedChunks", toJson(chunks))
                .param("latencyMs", Math.max(0, latencyMs))
                .param("intent", intent)
                .param("handoffRequired", handoffRequired)
                .param("fallbackReason", fallbackReason)
                .param("provider", provider)
                .param("responseMode", responseMode)
                .param("llmProvider", llmProvider)
                .param("model", model)
                .param("llmLatencyMs", llmLatencyMs)
                .param("promptTokens", promptTokens)
                .param("completionTokens", completionTokens)
                .param("totalTokens", totalTokens)
                .update();
    }

    private String toJson(List<RetrievedChunk> chunks) {
        List<Map<String, Object>> payload = chunks.stream()
                .map(chunk -> Map.<String, Object>of(
                        "chunkId", chunk.chunkId(),
                        "documentId", chunk.documentId(),
                        "filename", chunk.filename(),
                        "score", chunk.score()))
                .toList();
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar os chunks recuperados.", exception);
        }
    }
}
