package com.opiagile.supportai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.opiagile.supportai.tenant.TenantContext;

class ControlledToolPlannerTest {

    private final SqlReadOnlyToolService sqlReadOnlyToolService = Mockito.mock(SqlReadOnlyToolService.class);
    private final ToolPlanProvider toolPlanProvider = Mockito.mock(ToolPlanProvider.class);
    private final ControlledToolPlanner planner = new ControlledToolPlanner(sqlReadOnlyToolService, toolPlanProvider);
    private final TenantContext tenantContext = new TenantContext(
            UUID.randomUUID(),
            "opiagile",
            UUID.randomUUID(),
            "opiagile-rag",
            "Opiagile RAG");
    private final ExternalToolRecord tool = new ExternalToolRecord(
            UUID.randomUUID(),
            tenantContext.tenantId(),
            tenantContext.workspaceId(),
            "base-conhecimento-readonly",
            "Base de conhecimento somente leitura",
            "SQL_READ_ONLY",
            "ACTIVE",
            "Consulta segura",
            List.of("documents", "document_chunks", "retrieval_logs"),
            20,
            50);

    @Test
    void deveExecutarConsultaPlanejadaParaQuantidadeDeDocumentos() {
        when(sqlReadOnlyToolService.execute(eq(tenantContext), eq(tool.slug()), any(SqlToolExecutionRequest.class)))
                .thenReturn(new SqlToolExecutionResponse(
                        tool.slug(),
                        1,
                        false,
                        12,
                        List.of("status", "total"),
                        List.of(Map.of("status", "INDEXED", "total", 3))));

        List<ToolExecutionResult> results = planner.executeIfUseful(
                tenantContext,
                "Quantos documentos existem nesta base?",
                List.of(tool));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo("SUCCESS");
        assertThat(results.getFirst().rows()).containsExactly(Map.of("status", "INDEXED", "total", 3));

        ArgumentCaptor<SqlToolExecutionRequest> request = ArgumentCaptor.forClass(SqlToolExecutionRequest.class);
        verify(sqlReadOnlyToolService).execute(eq(tenantContext), eq(tool.slug()), request.capture());
        assertThat(request.getValue().sql()).contains("from documents");
        assertThat(request.getValue().sql()).contains("tenant_id = '" + tenantContext.tenantId() + "'");
        assertThat(request.getValue().sql()).contains("workspace_id = '" + tenantContext.workspaceId() + "'");
        verify(toolPlanProvider, never()).decide(any(), any());
    }

    @Test
    void naoDeveExecutarFerramentaParaPerguntaComum() {
        List<ToolExecutionResult> results = planner.executeIfUseful(
                tenantContext,
                "Vocês atendem aos sábados?",
                List.of(tool));

        assertThat(results).isEmpty();
        verify(sqlReadOnlyToolService, never()).execute(any(), any(), any());
        verify(toolPlanProvider, never()).decide(any(), any());
    }

    @Test
    void deveRetornarErroControladoQuandoFerramentaFalha() {
        when(sqlReadOnlyToolService.execute(eq(tenantContext), eq(tool.slug()), any(SqlToolExecutionRequest.class)))
                .thenThrow(new IllegalArgumentException("Falha simulada"));

        List<ToolExecutionResult> results = planner.executeIfUseful(
                tenantContext,
                "Liste os documentos indexados",
                List.of(tool));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo("ERROR");
        assertThat(results.getFirst().errorMessage()).contains("Falha simulada");
    }

    @Test
    void deveUsarPlannerLlmParaPerguntaOperacionalNaoCobertaPorHeuristica() {
        when(toolPlanProvider.decide(eq("Como está a base operacional deste workspace?"), eq(List.of(tool))))
                .thenReturn(new ToolPlanDecision(ToolPlanAction.DOCUMENT_COUNT, 0.9, "Pergunta operacional sobre tamanho da base."));
        when(sqlReadOnlyToolService.execute(eq(tenantContext), eq(tool.slug()), any(SqlToolExecutionRequest.class)))
                .thenReturn(new SqlToolExecutionResponse(
                        tool.slug(),
                        1,
                        false,
                        8,
                        List.of("status", "total"),
                        List.of(Map.of("status", "INDEXED", "total", 1))));

        List<ToolExecutionResult> results = planner.executeIfUseful(
                tenantContext,
                "Como está a base operacional deste workspace?",
                List.of(tool));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo("SUCCESS");
        verify(toolPlanProvider).decide(eq("Como está a base operacional deste workspace?"), eq(List.of(tool)));
    }

    @Test
    void naoDeveExecutarQuandoPlannerLlmRetornaNone() {
        when(toolPlanProvider.decide(eq("Quais dados vocês conseguem conectar?"), eq(List.of(tool))))
                .thenReturn(ToolPlanDecision.none("Pergunta comercial, não operacional."));

        List<ToolExecutionResult> results = planner.executeIfUseful(
                tenantContext,
                "Quais dados vocês conseguem conectar?",
                List.of(tool));

        assertThat(results).isEmpty();
        verify(sqlReadOnlyToolService, never()).execute(any(), any(), any());
    }

    @Test
    void deveTratarTamanhoDaBaseComoContagemDeDocumentos() {
        when(sqlReadOnlyToolService.execute(eq(tenantContext), eq(tool.slug()), any(SqlToolExecutionRequest.class)))
                .thenReturn(new SqlToolExecutionResponse(
                        tool.slug(),
                        1,
                        false,
                        7,
                        List.of("status", "total"),
                        List.of(Map.of("status", "INDEXED", "total", 1))));

        List<ToolExecutionResult> results = planner.executeIfUseful(
                tenantContext,
                "Qual é o tamanho da base deste workspace?",
                List.of(tool));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status()).isEqualTo("SUCCESS");
        verify(toolPlanProvider, never()).decide(any(), any());
    }
}
