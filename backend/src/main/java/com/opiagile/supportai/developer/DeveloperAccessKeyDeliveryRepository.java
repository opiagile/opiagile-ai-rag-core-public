package com.opiagile.supportai.developer;

import java.sql.Array;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DeveloperAccessKeyDeliveryRepository {

    private final JdbcClient jdbc;

    public DeveloperAccessKeyDeliveryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    UUID save(
            UUID requestId,
            UUID apiClientId,
            String tokenHash,
            String encryptedApiKey,
            String keyPrefix,
            String tenantSlug,
            String workspaceSlug,
            Set<String> scopes,
            int rateLimitPerMinute,
            OffsetDateTime sandboxExpiresAt,
            OffsetDateTime deliveryExpiresAt,
            String retentionNotice) {
        return jdbc.sql("""
                        INSERT INTO developer_access_key_deliveries
                            (request_id, api_client_id, token_hash, encrypted_api_key, key_prefix,
                             tenant_slug, workspace_slug, scopes, rate_limit_per_minute, sandbox_expires_at,
                             delivery_expires_at, retention_notice)
                        VALUES
                            (:requestId, :apiClientId, :tokenHash, :encryptedApiKey, :keyPrefix,
                             :tenantSlug, :workspaceSlug, string_to_array(:scopesCsv, ',')::text[], :rateLimit,
                             :sandboxExpiresAt, :deliveryExpiresAt, :retentionNotice)
                        RETURNING id
                        """)
                .param("requestId", requestId)
                .param("apiClientId", apiClientId)
                .param("tokenHash", tokenHash)
                .param("encryptedApiKey", encryptedApiKey)
                .param("keyPrefix", keyPrefix)
                .param("tenantSlug", tenantSlug)
                .param("workspaceSlug", workspaceSlug)
                .param("scopesCsv", String.join(",", scopes))
                .param("rateLimit", rateLimitPerMinute)
                .param("sandboxExpiresAt", sandboxExpiresAt)
                .param("deliveryExpiresAt", deliveryExpiresAt)
                .param("retentionNotice", retentionNotice)
                .query(UUID.class)
                .single();
    }

    boolean available(String tokenHash) {
        return Boolean.TRUE.equals(jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM developer_access_key_deliveries
                            WHERE token_hash = :tokenHash
                              AND consumed_at IS NULL
                              AND delivery_expires_at > now()
                        )
                        """)
                .param("tokenHash", tokenHash)
                .query(Boolean.class)
                .single());
    }

    Optional<DeveloperAccessKeyDelivery> consume(String tokenHash) {
        return jdbc.sql("""
                        UPDATE developer_access_key_deliveries
                        SET consumed_at = now(),
                            updated_at = now()
                        WHERE token_hash = :tokenHash
                          AND consumed_at IS NULL
                          AND delivery_expires_at > now()
                        RETURNING id, request_id, api_client_id, encrypted_api_key, key_prefix,
                                  tenant_slug, workspace_slug, scopes, rate_limit_per_minute,
                                  sandbox_expires_at, delivery_expires_at, retention_notice
                        """)
                .param("tokenHash", tokenHash)
                .query(this::map)
                .optional();
    }

    private DeveloperAccessKeyDelivery map(java.sql.ResultSet rs, int rowNum) throws SQLException {
        return new DeveloperAccessKeyDelivery(
                rs.getObject("id", UUID.class),
                rs.getObject("request_id", UUID.class),
                rs.getObject("api_client_id", UUID.class),
                rs.getString("encrypted_api_key"),
                rs.getString("key_prefix"),
                rs.getString("tenant_slug"),
                rs.getString("workspace_slug"),
                scopes(rs.getArray("scopes")),
                rs.getInt("rate_limit_per_minute"),
                rs.getObject("sandbox_expires_at", OffsetDateTime.class),
                rs.getObject("delivery_expires_at", OffsetDateTime.class),
                rs.getString("retention_notice"));
    }

    private Set<String> scopes(Array array) throws SQLException {
        if (array == null) {
            return Set.of();
        }
        String[] values = (String[]) array.getArray();
        return Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
