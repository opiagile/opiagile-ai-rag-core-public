package com.opiagile.supportai.observability;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ObservabilityRepository {

    private final JdbcClient jdbc;

    public ObservabilityRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<RetrievalTraceResponse> findRetrievals(UUID conversationId) {
        return jdbc.sql("""
                SELECT id, conversation_id, query, retrieved_chunks::text AS retrieved_chunks, latency_ms,
                       intent, handoff_required, fallback_reason, provider, response_mode, llm_provider, model,
                       llm_latency_ms, prompt_tokens, completion_tokens, total_tokens, created_at
                FROM retrieval_logs
                WHERE conversation_id = :conversationId
                ORDER BY created_at ASC
                """)
                .param("conversationId", conversationId)
                .query(this::map)
                .list();
    }

    private RetrievalTraceResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new RetrievalTraceResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("conversation_id", UUID.class),
                rs.getString("query"),
                rs.getString("retrieved_chunks"),
                rs.getInt("latency_ms"),
                rs.getString("intent"),
                rs.getBoolean("handoff_required"),
                rs.getString("fallback_reason"),
                rs.getString("provider"),
                rs.getString("response_mode"),
                rs.getString("llm_provider"),
                rs.getString("model"),
                getInteger(rs, "llm_latency_ms"),
                getInteger(rs, "prompt_tokens"),
                getInteger(rs, "completion_tokens"),
                getInteger(rs, "total_tokens"),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }
}
