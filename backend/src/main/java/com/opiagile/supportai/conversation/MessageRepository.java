package com.opiagile.supportai.conversation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

    private final JdbcClient jdbc;

    public MessageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public MessageRecord save(UUID conversationId, String role, String content, String intent) {
        return jdbc.sql("""
                INSERT INTO messages (conversation_id, role, content, intent)
                VALUES (:conversationId, :role, :content, :intent)
                RETURNING id, conversation_id, role, content, intent, created_at
                """)
                .param("conversationId", conversationId)
                .param("role", role)
                .param("content", content)
                .param("intent", intent)
                .query(this::map)
                .single();
    }

    public List<MessageRecord> findRecent(UUID conversationId, int limit) {
        return jdbc.sql("""
                SELECT id, conversation_id, role, content, intent, created_at
                FROM messages
                WHERE conversation_id = :conversationId
                ORDER BY created_at DESC
                LIMIT :limit
                """)
                .param("conversationId", conversationId)
                .param("limit", Math.max(1, limit))
                .query(this::map)
                .list()
                .reversed();
    }

    public List<MessageRecord> findByConversationId(UUID conversationId) {
        return jdbc.sql("""
                SELECT id, conversation_id, role, content, intent, created_at
                FROM messages
                WHERE conversation_id = :conversationId
                ORDER BY created_at ASC
                """)
                .param("conversationId", conversationId)
                .query(this::map)
                .list();
    }

    private MessageRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new MessageRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("conversation_id", UUID.class),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("intent"),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
