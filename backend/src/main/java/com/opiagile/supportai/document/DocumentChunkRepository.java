package com.opiagile.supportai.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.opiagile.supportai.rag.EmbeddingVectorCodec;
import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class DocumentChunkRepository {

    private final JdbcClient jdbc;

    public DocumentChunkRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public DocumentChunkRecord save(UUID documentId, int chunkIndex, String content, String metadata) {
        return save(documentId, chunkIndex, content, metadata, Optional.empty());
    }

    public DocumentChunkRecord save(UUID documentId, int chunkIndex, String content, String metadata, Optional<float[]> embedding) {
        if (embedding.isPresent()) {
            return saveWithEmbedding(documentId, chunkIndex, content, metadata, embedding.get());
        }
        return jdbc.sql("""
                INSERT INTO document_chunks (document_id, chunk_index, content, metadata, embedding)
                VALUES (:documentId, :chunkIndex, :content, CAST(:metadata AS jsonb), NULL)
                RETURNING id, document_id, chunk_index, content, metadata::text AS metadata, created_at
                """)
                .param("documentId", documentId)
                .param("chunkIndex", chunkIndex)
                .param("content", content)
                .param("metadata", metadata)
                .query(this::map)
                .single();
    }

    private DocumentChunkRecord saveWithEmbedding(UUID documentId, int chunkIndex, String content, String metadata, float[] embedding) {
        return jdbc.sql("""
                INSERT INTO document_chunks (document_id, chunk_index, content, metadata, embedding)
                VALUES (:documentId, :chunkIndex, :content, CAST(:metadata AS jsonb), CAST(:embedding AS vector))
                RETURNING id, document_id, chunk_index, content, metadata::text AS metadata, created_at
                """)
                .param("documentId", documentId)
                .param("chunkIndex", chunkIndex)
                .param("content", content)
                .param("metadata", metadata)
                .param("embedding", EmbeddingVectorCodec.toPgVector(embedding))
                .query(this::map)
                .single();
    }

    public List<DocumentChunkRecord> findByDocumentId(TenantContext tenantContext, UUID documentId) {
        return jdbc.sql("""
                SELECT dc.id, dc.document_id, dc.chunk_index, dc.content, dc.metadata::text AS metadata, dc.created_at
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE dc.document_id = :documentId
                  AND d.tenant_id = :tenantId
                  AND d.workspace_id = :workspaceId
                ORDER BY chunk_index
                """)
                .param("documentId", documentId)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .query(this::map)
                .list();
    }

    private DocumentChunkRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentChunkRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getString("metadata"),
                rs.getObject("created_at", java.time.OffsetDateTime.class));
    }
}
