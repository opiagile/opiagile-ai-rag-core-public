package com.opiagile.supportai.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConversationMemoryServiceTest {

    private final ConversationMemoryService memoryService = new ConversationMemoryService(4);

    @Test
    void deveCombinarHistoricoDoUsuarioComMensagemAtual() {
        UUID conversationId = UUID.randomUUID();
        List<MessageRecord> history = List.of(
                message(conversationId, "USER", "Qual o horário de atendimento?"),
                message(conversationId, "ASSISTANT", "Encontrei horários na base."));

        String query = memoryService.buildRetrievalQuery(history, "E aos sábados?");

        assertThat(query).contains("Qual o horário de atendimento?");
        assertThat(query).contains("E aos sábados?");
    }

    @Test
    void deveGerarResumoSimplesDaConversa() {
        UUID conversationId = UUID.randomUUID();
        String summary = memoryService.summarize(List.of(
                message(conversationId, "USER", "Meu nome é João."),
                message(conversationId, "ASSISTANT", "Certo."),
                message(conversationId, "USER", "Quero agendar consulta.")));

        assertThat(summary).contains("2 mensagens do usuário");
        assertThat(summary).contains("Quero agendar consulta.");
    }

    private MessageRecord message(UUID conversationId, String role, String content) {
        return new MessageRecord(UUID.randomUUID(), conversationId, role, content, null, OffsetDateTime.now());
    }
}
