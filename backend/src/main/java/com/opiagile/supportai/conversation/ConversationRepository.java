package com.opiagile.supportai.conversation;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class ConversationRepository {

    private final JdbcClient jdbc;

    public ConversationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID ensureConversation(TenantContext tenantContext, UUID requestedId, String channel, String contactId) {
        UUID conversationId = requestedId == null ? UUID.randomUUID() : requestedId;
        return jdbc.sql("""
                INSERT INTO conversations (id, tenant_id, workspace_id, external_channel, external_contact_id, status)
                VALUES (:id, :tenantId, :workspaceId, :channel, :contactId, 'ACTIVE')
                ON CONFLICT (id) DO UPDATE SET
                    external_channel = EXCLUDED.external_channel,
                    external_contact_id = COALESCE(EXCLUDED.external_contact_id, conversations.external_contact_id),
                    updated_at = now()
                WHERE conversations.tenant_id = EXCLUDED.tenant_id
                  AND conversations.workspace_id = EXCLUDED.workspace_id
                RETURNING id
                """)
                .param("id", conversationId)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("channel", channel)
                .param("contactId", contactId)
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new IllegalArgumentException("Conversa não pertence ao tenant/workspace informado."));
    }
}
