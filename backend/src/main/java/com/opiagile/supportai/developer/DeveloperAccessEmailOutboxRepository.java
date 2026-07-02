package com.opiagile.supportai.developer;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DeveloperAccessEmailOutboxRepository {

    private final JdbcClient jdbc;

    public DeveloperAccessEmailOutboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    void enqueue(UUID requestId, String emailType, String recipient, String subject, String htmlBody, String textBody) {
        jdbc.sql("""
                        INSERT INTO developer_access_email_outbox
                            (request_id, email_type, recipient, subject, html_body, text_body)
                        VALUES
                            (:requestId, :emailType, :recipient, :subject, :htmlBody, :textBody)
                        """)
                .param("requestId", requestId)
                .param("emailType", emailType)
                .param("recipient", normalize(recipient))
                .param("subject", truncate(normalize(subject), 220))
                .param("htmlBody", normalize(htmlBody))
                .param("textBody", normalize(textBody))
                .update();
    }

    List<DeveloperAccessEmailMessage> findPending(int limit, int maxAttempts) {
        return jdbc.sql("""
                        SELECT id, request_id, email_type, recipient, subject, html_body, text_body
                        FROM developer_access_email_outbox
                        WHERE status = 'PENDING'
                          AND attempts < :maxAttempts
                          AND next_attempt_at <= now()
                        ORDER BY created_at ASC
                        LIMIT :limit
                        """)
                .param("maxAttempts", maxAttempts)
                .param("limit", limit)
                .query((rs, rowNum) -> new DeveloperAccessEmailMessage(
                        rs.getObject("id", UUID.class),
                        rs.getObject("request_id", UUID.class),
                        rs.getString("email_type"),
                        rs.getString("recipient"),
                        rs.getString("subject"),
                        rs.getString("html_body"),
                        rs.getString("text_body")))
                .list();
    }

    void markSent(UUID emailId) {
        jdbc.sql("""
                        UPDATE developer_access_email_outbox
                        SET status = 'SENT',
                            sent_at = now(),
                            last_attempt_at = now(),
                            attempts = attempts + 1,
                            last_error = null,
                            updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", emailId)
                .update();
    }

    void markFailed(UUID emailId, String failureReason, int retryDelayMinutes) {
        jdbc.sql("""
                        UPDATE developer_access_email_outbox
                        SET status = 'PENDING',
                            last_attempt_at = now(),
                            attempts = attempts + 1,
                            next_attempt_at = now() + (:retryDelayMinutes * INTERVAL '1 minute'),
                            last_error = :failureReason,
                            updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", emailId)
                .param("retryDelayMinutes", retryDelayMinutes)
                .param("failureReason", truncate(failureReason, 240))
                .update();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
