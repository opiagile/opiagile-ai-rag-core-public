package com.opiagile.supportai.tool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.opiagile.supportai.tenant.TenantContext;

@Service
public class SqlReadOnlyToolService {

    private final DataSource dataSource;
    private final ExternalToolRepository repository;
    private final SqlReadOnlyGuard guard;

    public SqlReadOnlyToolService(DataSource dataSource, ExternalToolRepository repository, SqlReadOnlyGuard guard) {
        this.dataSource = dataSource;
        this.repository = repository;
        this.guard = guard;
    }

    public SqlToolExecutionResponse execute(TenantContext tenantContext, String toolSlug, SqlToolExecutionRequest request) {
        Instant startedAt = Instant.now();
        ExternalToolRecord tool = repository.findActive(tenantContext, toolSlug)
                .orElseThrow(() -> new IllegalArgumentException("Ferramenta ativa não encontrada: " + toolSlug));
        if (!"SQL_READ_ONLY".equals(tool.type())) {
            throw new IllegalArgumentException("Ferramenta não suporta execução SQL.");
        }
        int limit = effectiveLimit(request.maxRows(), tool);
        String query = guard.validate(request.sql(), tool, limit + 1);
        try {
            SqlToolExecutionResponse response = runQuery(tool, query, limit);
            repository.saveExecutionLog(
                    tenantContext,
                    tool,
                    "SUCCESS",
                    preview(request.sql()),
                    response.rowCount(),
                    response.latencyMs(),
                    null);
            return response;
        } catch (RuntimeException exception) {
            repository.saveExecutionLog(
                    tenantContext,
                    tool,
                    "ERROR",
                    preview(request.sql()),
                    0,
                    (int) Duration.between(startedAt, Instant.now()).toMillis(),
                    exception.getMessage());
            throw exception;
        }
    }

    private SqlToolExecutionResponse runQuery(ExternalToolRecord tool, String query, int limit) {
        Instant startedAt = Instant.now();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            connection.setReadOnly(true);
            statement.setQueryTimeout(8);
            statement.setFetchSize(Math.min(limit + 1, 100));
            try (ResultSet rs = statement.executeQuery(query)) {
                ResultSetMetaData metadata = rs.getMetaData();
                List<String> columns = columns(metadata);
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next() && rows.size() <= limit) {
                    rows.add(row(rs, columns));
                }
                boolean truncated = rows.size() > limit;
                if (truncated) {
                    rows = rows.subList(0, limit);
                }
                return new SqlToolExecutionResponse(
                        tool.slug(),
                        rows.size(),
                        truncated,
                        (int) Duration.between(startedAt, Instant.now()).toMillis(),
                        columns,
                        rows);
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Falha ao executar consulta somente leitura: " + exception.getMessage(), exception);
        }
    }

    private int effectiveLimit(Integer requestedLimit, ExternalToolRecord tool) {
        int baseLimit = requestedLimit == null ? tool.defaultLimit() : requestedLimit;
        return Math.max(1, Math.min(baseLimit, tool.maxLimit()));
    }

    private List<String> columns(ResultSetMetaData metadata) throws Exception {
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            columns.add(metadata.getColumnLabel(i));
        }
        return columns;
    }

    private Map<String, Object> row(ResultSet rs, List<String> columns) throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String column : columns) {
            row.put(column, rs.getObject(column));
        }
        return row;
    }

    private String preview(String sql) {
        if (sql == null) {
            return "";
        }
        String normalized = sql.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
    }
}
