package com.opiagile.supportai.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class DocumentRepository {

    private final JdbcClient jdbc;

    public DocumentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public DocumentRecord save(TenantContext tenantContext, String filename, String contentType, String sourceType, DocumentStatus status) {
        return jdbc.sql("""
                INSERT INTO documents (tenant_id, workspace_id, filename, content_type, source_type, status)
                VALUES (:tenantId, :workspaceId, :filename, :contentType, :sourceType, :status)
                RETURNING id,
                          (SELECT slug FROM tenants WHERE id = tenant_id) AS tenant_slug,
                          (SELECT slug FROM workspaces WHERE id = workspace_id) AS workspace_slug,
                          filename, content_type, source_type, status, created_at
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("filename", filename)
                .param("contentType", contentType)
                .param("sourceType", sourceType)
                .param("status", status.name())
                .query(this::map)
                .single();
    }

    public List<DocumentRecord> findAll(TenantContext tenantContext) {
        return jdbc.sql("""
                SELECT d.id, t.slug AS tenant_slug, w.slug AS workspace_slug,
                       d.filename, d.content_type, d.source_type, d.status, d.created_at
                FROM documents d
                JOIN tenants t ON t.id = d.tenant_id
                JOIN workspaces w ON w.id = d.workspace_id
                WHERE d.tenant_id = :tenantId
                  AND d.workspace_id = :workspaceId
                ORDER BY d.created_at DESC
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .query(this::map)
                .list();
    }

    public Optional<DocumentRecord> findById(TenantContext tenantContext, UUID id) {
        return jdbc.sql("""
                SELECT d.id, t.slug AS tenant_slug, w.slug AS workspace_slug,
                       d.filename, d.content_type, d.source_type, d.status, d.created_at
                FROM documents d
                JOIN tenants t ON t.id = d.tenant_id
                JOIN workspaces w ON w.id = d.workspace_id
                WHERE d.id = :id
                  AND d.tenant_id = :tenantId
                  AND d.workspace_id = :workspaceId
                """)
                .param("id", id)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .query(this::map)
                .optional();
    }

    public int countChunks(UUID documentId) {
        Integer count = jdbc.sql("SELECT count(*) FROM document_chunks WHERE document_id = :documentId")
                .param("documentId", documentId)
                .query(Integer.class)
                .single();
        return count == null ? 0 : count;
    }

    public int countDocuments(TenantContext tenantContext) {
        Integer count = jdbc.sql("""
                SELECT count(*)
                FROM documents
                WHERE tenant_id = :tenantId
                  AND workspace_id = :workspaceId
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .query(Integer.class)
                .single();
        return count == null ? 0 : count;
    }

    private DocumentRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentRecord(
                rs.getObject("id", UUID.class),
                rs.getString("tenant_slug"),
                rs.getString("workspace_slug"),
                rs.getString("filename"),
                rs.getString("content_type"),
                rs.getString("source_type"),
                rs.getString("status"),
                rs.getObject("created_at", java.time.OffsetDateTime.class));
    }
}
