package com.opiagile.supportai.tool;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.opiagile.supportai.tenant.TenantContext;

@Service
public class ControlledToolPlanner {

    private static final String KNOWLEDGE_BASE_TOOL = "base-conhecimento-readonly";

    private final SqlReadOnlyToolService sqlReadOnlyToolService;
    private final ToolPlanProvider toolPlanProvider;

    public ControlledToolPlanner(SqlReadOnlyToolService sqlReadOnlyToolService, ToolPlanProvider toolPlanProvider) {
        this.sqlReadOnlyToolService = sqlReadOnlyToolService;
        this.toolPlanProvider = toolPlanProvider;
    }

    public List<ToolExecutionResult> executeIfUseful(
            TenantContext tenantContext,
            String currentMessage,
            List<ExternalToolRecord> availableTools) {
        Optional<ExternalToolRecord> tool = knowledgeBaseTool(availableTools);
        if (tool.isEmpty()) {
            return List.of();
        }
        Optional<PlannedQuery> plannedQuery = deterministicPlan(tenantContext, currentMessage);
        if (plannedQuery.isEmpty() && mayNeedOperationalData(currentMessage)) {
            plannedQuery = llmPlan(tenantContext, currentMessage, availableTools);
        }
        if (plannedQuery.isEmpty()) {
            return List.of();
        }
        PlannedQuery query = plannedQuery.get();
        try {
            SqlToolExecutionResponse response = sqlReadOnlyToolService.execute(
                    tenantContext,
                    tool.get().slug(),
                    new SqlToolExecutionRequest(query.sql(), query.maxRows()));
            return List.of(ToolExecutionResult.success(query.purpose(), query.sql(), response));
        } catch (RuntimeException exception) {
            return List.of(ToolExecutionResult.error(tool.get().slug(), query.purpose(), query.sql(), exception.getMessage()));
        }
    }

    private Optional<ExternalToolRecord> knowledgeBaseTool(List<ExternalToolRecord> tools) {
        if (tools == null || tools.isEmpty()) {
            return Optional.empty();
        }
        return tools.stream()
                .filter(tool -> KNOWLEDGE_BASE_TOOL.equals(tool.slug()))
                .filter(tool -> "ACTIVE".equals(tool.status()))
                .filter(tool -> "SQL_READ_ONLY".equals(tool.type()))
                .findFirst();
    }

    private Optional<PlannedQuery> deterministicPlan(TenantContext tenantContext, String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        if (mentionsRecentRetrievals(normalized)) {
            return Optional.of(queryFor(tenantContext, ToolPlanAction.RECENT_RETRIEVALS));
        }
        if (mentionsChunkStats(normalized)) {
            return Optional.of(queryFor(tenantContext, ToolPlanAction.CHUNK_STATS));
        }
        if (mentionsDocumentList(normalized)) {
            return Optional.of(queryFor(tenantContext, ToolPlanAction.DOCUMENT_LIST));
        }
        if (mentionsDocumentCount(normalized)) {
            return Optional.of(queryFor(tenantContext, ToolPlanAction.DOCUMENT_COUNT));
        }
        return Optional.empty();
    }

    private Optional<PlannedQuery> llmPlan(
            TenantContext tenantContext,
            String currentMessage,
            List<ExternalToolRecord> availableTools) {
        ToolPlanDecision decision = toolPlanProvider.decide(currentMessage, availableTools);
        if (decision.action() == ToolPlanAction.NONE) {
            return Optional.empty();
        }
        return Optional.of(queryFor(tenantContext, decision.action()));
    }

    private PlannedQuery queryFor(TenantContext tenantContext, ToolPlanAction action) {
        String tenantId = sqlUuid(tenantContext.tenantId());
        String workspaceId = sqlUuid(tenantContext.workspaceId());
        return switch (action) {
            case RECENT_RETRIEVALS -> new PlannedQuery(
                    "Consultar perguntas recentes feitas neste workspace.",
                    """
                            select query, intent, response_mode, provider, created_at
                            from retrieval_logs
                            where tenant_id = '%s' and workspace_id = '%s'
                            order by created_at desc
                            """.formatted(tenantId, workspaceId),
                    10);
            case CHUNK_STATS -> new PlannedQuery(
                    "Contar trechos indexados por documento neste workspace.",
                    """
                            select d.filename, count(dc.id) as total_trechos
                            from documents d
                            join document_chunks dc on dc.document_id = d.id
                            where d.tenant_id = '%s' and d.workspace_id = '%s'
                            group by d.filename
                            order by total_trechos desc, d.filename
                            """.formatted(tenantId, workspaceId),
                    20);
            case DOCUMENT_LIST -> new PlannedQuery(
                    "Listar documentos indexados neste workspace.",
                    """
                            select filename, status, source_type, content_type, created_at
                            from documents
                            where tenant_id = '%s' and workspace_id = '%s'
                            order by created_at desc
                            """.formatted(tenantId, workspaceId),
                    20);
            case DOCUMENT_COUNT -> new PlannedQuery(
                    "Contar documentos por status neste workspace.",
                    """
                            select status, count(*) as total
                            from documents
                            where tenant_id = '%s' and workspace_id = '%s'
                            group by status
                            order by status
                            """.formatted(tenantId, workspaceId),
                    20);
            case NONE -> throw new IllegalArgumentException("Ação NONE não gera consulta.");
        };
    }

    private boolean mentionsDocumentCount(String normalized) {
        return hasAny(normalized, "quantos", "quantidade", "total", "tamanho", "volume", "count", "how many", "size", "volume", "cuantos", "cantidad", "tamano")
                && hasAny(normalized, "base", "workspace", "documento", "documentos", "arquivo", "arquivos", "file", "files", "document", "documents");
    }

    private boolean mentionsDocumentList(String normalized) {
        return hasAny(normalized, "quais", "liste", "listar", "mostre", "mostrar", "list", "show", "which", "cuales", "mostrar")
                && hasAny(normalized, "documento", "documentos", "arquivo", "arquivos", "file", "files", "document", "documents");
    }

    private boolean mentionsChunkStats(String normalized) {
        return hasAny(normalized, "chunk", "chunks", "trecho", "trechos", "fragmento", "fragmentos", "pedaco", "pedacos", "excerpt", "excerpts");
    }

    private boolean mentionsRecentRetrievals(String normalized) {
        return hasAny(normalized, "perguntas recentes", "consultas recentes", "ultimas perguntas", "ultimas consultas",
                "recent questions", "recent queries", "preguntas recientes", "consultas recientes");
    }

    private boolean mayNeedOperationalData(String message) {
        String normalized = normalize(message);
        return hasAny(normalized,
                "base", "workspace", "documento", "documentos", "arquivo", "arquivos", "dados", "consulta", "consultas",
                "tamanho", "volume", "file", "files", "document", "documents", "data", "query", "queries", "size",
                "base", "workspace", "documento", "documentos", "archivo", "archivos", "datos", "consulta", "consultas");
    }

    private boolean hasAny(String normalized, String... terms) {
        for (String term : terms) {
            if (normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sqlUuid(UUID value) {
        return value.toString();
    }

    private record PlannedQuery(String purpose, String sql, int maxRows) {
    }
}
