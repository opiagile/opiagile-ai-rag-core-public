package com.opiagile.supportai.rag;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class RagChunkRepository {

    private final JdbcClient jdbc;

    public RagChunkRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<StoredChunk> findAllIndexedChunks(TenantContext tenantContext) {
        return jdbc.sql("""
                SELECT dc.id AS chunk_id, dc.document_id, d.filename, dc.content, dc.metadata ->> 'language' AS language
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE d.status = 'INDEXED'
                  AND d.tenant_id = :tenantId
                  AND d.workspace_id = :workspaceId
                ORDER BY d.created_at DESC, dc.chunk_index ASC
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .query(this::map)
                .list();
    }

    public List<StoredChunk> findNearestIndexedChunks(TenantContext tenantContext, float[] embedding, int limit) {
        return jdbc.sql("""
                SELECT dc.id AS chunk_id,
                       dc.document_id,
                       d.filename,
                       dc.content,
                       dc.metadata ->> 'language' AS language,
                       1 - (dc.embedding <=> CAST(:embedding AS vector)) AS score
                FROM document_chunks dc
                JOIN documents d ON d.id = dc.document_id
                WHERE d.status = 'INDEXED'
                  AND dc.embedding IS NOT NULL
                  AND d.tenant_id = :tenantId
                  AND d.workspace_id = :workspaceId
                ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                LIMIT :limit
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("embedding", EmbeddingVectorCodec.toPgVector(embedding))
                .param("limit", Math.max(1, limit))
                .query(this::mapWithScore)
                .list();
    }

    private StoredChunk map(ResultSet rs, int rowNum) throws SQLException {
        return new StoredChunk(
                rs.getObject("chunk_id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getString("filename"),
                rs.getString("content"),
                rs.getString("language"),
                null);
    }

    private StoredChunk mapWithScore(ResultSet rs, int rowNum) throws SQLException {
        return new StoredChunk(
                rs.getObject("chunk_id", UUID.class),
                rs.getObject("document_id", UUID.class),
                rs.getString("filename"),
                rs.getString("content"),
                rs.getString("language"),
                rs.getDouble("score"));
    }
}
