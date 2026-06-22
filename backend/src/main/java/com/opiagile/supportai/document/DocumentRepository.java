package com.opiagile.supportai.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentRepository {

    private final JdbcClient jdbc;

    public DocumentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public DocumentRecord save(String filename, String contentType, String sourceType, DocumentStatus status) {
        return jdbc.sql("""
                INSERT INTO documents (filename, content_type, source_type, status)
                VALUES (:filename, :contentType, :sourceType, :status)
                RETURNING id, filename, content_type, source_type, status, created_at
                """)
                .param("filename", filename)
                .param("contentType", contentType)
                .param("sourceType", sourceType)
                .param("status", status.name())
                .query(this::map)
                .single();
    }

    public List<DocumentRecord> findAll() {
        return jdbc.sql("""
                SELECT id, filename, content_type, source_type, status, created_at
                FROM documents
                ORDER BY created_at DESC
                """)
                .query(this::map)
                .list();
    }

    public Optional<DocumentRecord> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, filename, content_type, source_type, status, created_at
                FROM documents
                WHERE id = :id
                """)
                .param("id", id)
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

    public int countDocuments() {
        Integer count = jdbc.sql("SELECT count(*) FROM documents")
                .query(Integer.class)
                .single();
        return count == null ? 0 : count;
    }

    private DocumentRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentRecord(
                rs.getObject("id", UUID.class),
                rs.getString("filename"),
                rs.getString("content_type"),
                rs.getString("source_type"),
                rs.getString("status"),
                rs.getObject("created_at", java.time.OffsetDateTime.class));
    }
}
