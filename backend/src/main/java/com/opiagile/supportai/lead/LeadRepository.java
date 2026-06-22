package com.opiagile.supportai.lead;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class LeadRepository {

    private final JdbcClient jdbc;

    public LeadRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void upsertSummary(UUID conversationId, String status, String summary) {
        upsert(conversationId, status, summary, new LeadExtraction(null, null, null, null));
    }

    public void upsert(UUID conversationId, String status, String summary, LeadExtraction extraction) {
        jdbc.sql("""
                INSERT INTO leads (conversation_id, name, phone, email, interest, status, summary)
                VALUES (:conversationId, :name, :phone, :email, :interest, :status, :summary)
                ON CONFLICT (conversation_id) DO UPDATE SET
                    name = COALESCE(EXCLUDED.name, leads.name),
                    phone = COALESCE(EXCLUDED.phone, leads.phone),
                    email = COALESCE(EXCLUDED.email, leads.email),
                    interest = COALESCE(EXCLUDED.interest, leads.interest),
                    status = EXCLUDED.status,
                    summary = EXCLUDED.summary,
                    updated_at = now()
                """)
                .param("conversationId", conversationId)
                .param("name", extraction.name())
                .param("phone", extraction.phone())
                .param("email", extraction.email())
                .param("interest", extraction.interest())
                .param("status", status)
                .param("summary", summary)
                .update();
    }
}
