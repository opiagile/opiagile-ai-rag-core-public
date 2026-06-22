package com.opiagile.supportai.handoff;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class HandoffRepository {

    private final JdbcClient jdbc;

    public HandoffRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public HandoffRecord create(UUID conversationId, String reason, String summary) {
        return jdbc.sql("""
                INSERT INTO handoff_requests (conversation_id, reason, summary, status)
                VALUES (:conversationId, :reason, :summary, :status)
                RETURNING id, conversation_id, reason, summary, status, created_at
                """)
                .param("conversationId", conversationId)
                .param("reason", reason)
                .param("summary", summary)
                .param("status", HandoffStatus.OPEN.name())
                .query(this::map)
                .single();
    }

    public Optional<HandoffRecord> findOpenByConversationId(UUID conversationId) {
        return jdbc.sql("""
                SELECT id, conversation_id, reason, summary, status, created_at
                FROM handoff_requests
                WHERE conversation_id = :conversationId
                  AND status IN ('OPEN', 'IN_PROGRESS')
                ORDER BY created_at DESC
                LIMIT 1
                """)
                .param("conversationId", conversationId)
                .query(this::map)
                .optional();
    }

    public List<HandoffRecord> findAll() {
        return jdbc.sql("""
                SELECT id, conversation_id, reason, summary, status, created_at
                FROM handoff_requests
                ORDER BY created_at DESC
                """)
                .query(this::map)
                .list();
    }

    public Optional<HandoffRecord> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, conversation_id, reason, summary, status, created_at
                FROM handoff_requests
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::map)
                .optional();
    }

    public HandoffRecord updateStatus(UUID id, HandoffStatus status) {
        return jdbc.sql("""
                UPDATE handoff_requests
                SET status = :status
                WHERE id = :id
                RETURNING id, conversation_id, reason, summary, status, created_at
                """)
                .param("id", id)
                .param("status", status.name())
                .query(this::map)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Handoff não encontrado: " + id));
    }

    private HandoffRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new HandoffRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("conversation_id", UUID.class),
                rs.getString("reason"),
                rs.getString("summary"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
