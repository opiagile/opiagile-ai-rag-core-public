package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DeveloperAccessRequestRepository {

    private final JdbcClient jdbcClient;

    public DeveloperAccessRequestRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    DeveloperAccessRequestRecord save(DeveloperAccessRequest request, String clientIp, String userAgent) {
        return jdbcClient.sql("""
                        INSERT INTO developer_access_requests
                            (name, company, email, use_case, requested_resources, source, client_ip, user_agent)
                        VALUES
                            (:name, :company, :email, :useCase, :requestedResources, 'developers-portal', :clientIp, :userAgent)
                        RETURNING id, created_at
                        """)
                .param("name", normalize(request.name()))
                .param("company", nullable(request.company()))
                .param("email", normalize(request.email()).toLowerCase())
                .param("useCase", normalize(request.useCase()))
                .param("requestedResources", nullable(request.requestedResources()))
                .param("clientIp", nullable(clientIp))
                .param("userAgent", nullable(truncate(userAgent, 220)))
                .query((rs, rowNum) -> new DeveloperAccessRequestRecord(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)))
                .single();
    }

    List<DeveloperAccessNotificationRequest> findPendingEmailNotifications(int limit, int maxAttempts) {
        return jdbcClient.sql("""
                        SELECT id, created_at, name, company, email, use_case, requested_resources
                        FROM developer_access_requests
                        WHERE notification_email_sent = false
                          AND status = 'NEW'
                          AND notification_email_attempts < :maxAttempts
                          AND notification_email_next_attempt_at <= now()
                        ORDER BY created_at ASC
                        LIMIT :limit
                        """)
                .param("maxAttempts", maxAttempts)
                .param("limit", limit)
                .query((rs, rowNum) -> new DeveloperAccessNotificationRequest(
                        rs.getObject("id", UUID.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getString("name"),
                        rs.getString("company"),
                        rs.getString("email"),
                        rs.getString("use_case"),
                        rs.getString("requested_resources")))
                .list();
    }

    void markEmailSent(UUID requestId) {
        jdbcClient.sql("""
                        UPDATE developer_access_requests
                        SET notification_email_sent = true,
                            notification_email_sent_at = now(),
                            notification_email_last_attempt_at = now(),
                            notification_email_attempts = notification_email_attempts + 1,
                            notification_email_last_error = null,
                            updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", requestId)
                .update();
    }

    void markEmailFailed(UUID requestId, String failureReason, int retryDelayMinutes) {
        jdbcClient.sql("""
                        UPDATE developer_access_requests
                        SET notification_email_sent = false,
                            notification_email_last_attempt_at = now(),
                            notification_email_attempts = notification_email_attempts + 1,
                            notification_email_next_attempt_at = now() + (:retryDelayMinutes * INTERVAL '1 minute'),
                            notification_email_last_error = :failureReason,
                            updated_at = now()
                        WHERE id = :id
                        """)
                .param("id", requestId)
                .param("retryDelayMinutes", retryDelayMinutes)
                .param("failureReason", truncate(failureReason, 240))
                .update();
    }

    public List<DeveloperAccessRequestAdminResponse> findForAdmin(String status, int limit) {
        if (status == null || status.isBlank()) {
            return jdbcClient.sql("""
                            SELECT id, created_at, updated_at, name, company, email, use_case,
                                   requested_resources, source, status, notification_email_sent,
                                   notification_email_attempts, notification_email_last_error,
                                   approved_tenant_slug, approved_workspace_slug, sandbox_expires_at, sandbox_deleted_at
                            FROM developer_access_requests
                            ORDER BY created_at DESC
                            LIMIT :limit
                            """)
                    .param("limit", limit)
                    .query(this::mapAdminResponse)
                    .list();
        }
        return jdbcClient.sql("""
                        SELECT id, created_at, updated_at, name, company, email, use_case,
                               requested_resources, source, status, notification_email_sent,
                               notification_email_attempts, notification_email_last_error,
                               approved_tenant_slug, approved_workspace_slug, sandbox_expires_at, sandbox_deleted_at
                        FROM developer_access_requests
                        WHERE status = :status
                        ORDER BY created_at DESC
                        LIMIT :limit
                        """)
                .param("status", status)
                .param("limit", limit)
                .query(this::mapAdminResponse)
                .list();
    }

    public Optional<DeveloperAccessRequestAdminResponse> findById(UUID requestId) {
        return jdbcClient.sql("""
                        SELECT id, created_at, updated_at, name, company, email, use_case,
                               requested_resources, source, status, notification_email_sent,
                               notification_email_attempts, notification_email_last_error,
                               approved_tenant_slug, approved_workspace_slug, sandbox_expires_at, sandbox_deleted_at
                        FROM developer_access_requests
                        WHERE id = :id
                        """)
                .param("id", requestId)
                .query(this::mapAdminResponse)
                .optional();
    }

    public boolean markApproved(
            UUID requestId,
            UUID apiClientId,
            String tenantSlug,
            String workspaceSlug,
            OffsetDateTime sandboxExpiresAt) {
        int updated = jdbcClient.sql("""
                        UPDATE developer_access_requests
                        SET status = 'APPROVED',
                            approved_api_client_id = :apiClientId,
                            approved_tenant_slug = :tenantSlug,
                            approved_workspace_slug = :workspaceSlug,
                            sandbox_expires_at = :sandboxExpiresAt,
                            updated_at = now()
                        WHERE id = :id
                          AND status <> 'APPROVED'
                        """)
                .param("id", requestId)
                .param("apiClientId", apiClientId)
                .param("tenantSlug", tenantSlug)
                .param("workspaceSlug", workspaceSlug)
                .param("sandboxExpiresAt", sandboxExpiresAt)
                .update();
        return updated == 1;
    }

    public void markSandboxDeleted(String tenantSlug) {
        jdbcClient.sql("""
                        UPDATE developer_access_requests
                        SET sandbox_deleted_at = now(),
                            updated_at = now()
                        WHERE approved_tenant_slug = :tenantSlug
                          AND sandbox_deleted_at IS NULL
                        """)
                .param("tenantSlug", tenantSlug)
                .update();
    }

    private DeveloperAccessRequestAdminResponse mapAdminResponse(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DeveloperAccessRequestAdminResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                rs.getString("name"),
                rs.getString("company"),
                rs.getString("email"),
                rs.getString("use_case"),
                rs.getString("requested_resources"),
                rs.getString("source"),
                rs.getString("status"),
                rs.getBoolean("notification_email_sent"),
                rs.getInt("notification_email_attempts"),
                rs.getString("notification_email_last_error"),
                rs.getString("approved_tenant_slug"),
                rs.getString("approved_workspace_slug"),
                rs.getObject("sandbox_expires_at", OffsetDateTime.class),
                rs.getObject("sandbox_deleted_at", OffsetDateTime.class));
    }

    private String nullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
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
