package com.opiagile.supportai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SqlReadOnlyGuardTest {

    private final SqlReadOnlyGuard guard = new SqlReadOnlyGuard();
    private final ExternalToolRecord tool = new ExternalToolRecord(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "base-conhecimento-readonly",
            "Base de conhecimento somente leitura",
            "SQL_READ_ONLY",
            "ACTIVE",
            "Consulta segura",
            List.of("documents", "document_chunks", "retrieval_logs"),
            20,
            50);

    @Test
    void devePermitirSelectEmTabelaAutorizada() {
        String query = guard.validate("select filename, status from documents order by created_at desc", tool, 21);

        assertThat(query).startsWith("SELECT * FROM (select filename");
        assertThat(query).endsWith("LIMIT 21");
    }

    @Test
    void deveBloquearComandoDeEscrita() {
        assertThatThrownBy(() -> guard.validate("delete from documents", tool, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SELECT");
    }

    @Test
    void deveBloquearTabelaForaDaAllowlist() {
        assertThatThrownBy(() -> guard.validate("select * from leads", tool, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tabela não permitida");
    }

    @Test
    void deveBloquearMultiplosComandos() {
        assertThatThrownBy(() -> guard.validate("select * from documents; select * from leads", tool, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("múltiplos comandos");
    }
}
