package com.opiagile.supportai.conversation;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ConversationRepository {

    private final JdbcClient jdbc;

    public ConversationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID ensureConversation(UUID requestedId, String channel, String contactId) {
        UUID conversationId = requestedId == null ? UUID.randomUUID() : requestedId;
        return jdbc.sql("""
                INSERT INTO conversations (id, external_channel, external_contact_id, status)
                VALUES (:id, :channel, :contactId, 'ACTIVE')
                ON CONFLICT (id) DO UPDATE SET
                    external_channel = EXCLUDED.external_channel,
                    external_contact_id = COALESCE(EXCLUDED.external_contact_id, conversations.external_contact_id),
                    updated_at = now()
                RETURNING id
                """)
                .param("id", conversationId)
                .param("channel", channel)
                .param("contactId", contactId)
                .query(UUID.class)
                .single();
    }
}
