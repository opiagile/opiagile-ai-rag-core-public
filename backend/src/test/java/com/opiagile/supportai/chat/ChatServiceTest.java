package com.opiagile.supportai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.opiagile.supportai.conversation.ConversationMemoryService;
import com.opiagile.supportai.conversation.ConversationRepository;
import com.opiagile.supportai.conversation.MessageRecord;
import com.opiagile.supportai.conversation.MessageRepository;
import com.opiagile.supportai.handoff.HandoffService;
import com.opiagile.supportai.lead.Intent;
import com.opiagile.supportai.lead.IntentClassifier;
import com.opiagile.supportai.lead.LeadExtraction;
import com.opiagile.supportai.lead.LeadExtractor;
import com.opiagile.supportai.lead.LeadRepository;
import com.opiagile.supportai.rag.MultilingualQueryExpander;
import com.opiagile.supportai.rag.RagRetrievalService;
import com.opiagile.supportai.rag.RetrievalLogRepository;
import com.opiagile.supportai.rag.RetrievedChunk;
import com.opiagile.supportai.rag.TextSimilarityScorer;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tool.ControlledToolPlanner;
import com.opiagile.supportai.tool.ExternalToolRepository;

class ChatServiceTest {

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final LeadRepository leadRepository = mock(LeadRepository.class);
    private final IntentClassifier intentClassifier = mock(IntentClassifier.class);
    private final LeadExtractor leadExtractor = mock(LeadExtractor.class);
    private final HandoffService handoffService = mock(HandoffService.class);
    private final RagRetrievalService ragRetrievalService = mock(RagRetrievalService.class);
    private final RetrievalLogRepository retrievalLogRepository = mock(RetrievalLogRepository.class);
    private final ChatModelProvider chatModelProvider = mock(ChatModelProvider.class);
    private final ExternalToolRepository externalToolRepository = mock(ExternalToolRepository.class);
    private final ControlledToolPlanner controlledToolPlanner = mock(ControlledToolPlanner.class);

    private final ChatService service = new ChatService(
            conversationRepository,
            messageRepository,
            new ConversationMemoryService(8),
            leadRepository,
            intentClassifier,
            leadExtractor,
            handoffService,
            ragRetrievalService,
            retrievalLogRepository,
            chatModelProvider,
            new MultilingualQueryExpander(),
            new TextSimilarityScorer(),
            externalToolRepository,
            controlledToolPlanner,
            0.15);

    @Test
    void naoDeveChamarLlmQuandoPerguntaAtualNaoEstaNasFontes() {
        UUID conversationId = UUID.randomUUID();
        TenantContext tenantContext = new TenantContext(
                UUID.randomUUID(),
                "opiagile",
                UUID.randomUUID(),
                "opiagile-rag",
                "Assistente Opiagile");
        MessageRecord previousMessage = message(conversationId, "USER", "Como a Opiagile ajuda empresas?");
        RetrievedChunk unrelatedChunk = new RetrievedChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "opiagile-rag-conhecimento.md",
                "A Opiagile ajuda empresas a transformar documentos e FAQs em respostas com fontes.",
                0.82,
                "A Opiagile ajuda empresas a transformar documentos e FAQs em respostas com fontes.",
                "local-text");

        when(conversationRepository.ensureConversation(eq(tenantContext), any(), eq("WEB"), eq("teste")))
                .thenReturn(conversationId);
        when(messageRepository.findRecent(eq(conversationId), anyInt()))
                .thenReturn(List.of(previousMessage))
                .thenReturn(List.of(previousMessage, message(conversationId, "USER", "Quanto é 2+2?")));
        when(messageRepository.save(eq(conversationId), any(), any(), any()))
                .thenAnswer(invocation -> message(conversationId, invocation.getArgument(1), invocation.getArgument(2)));
        when(intentClassifier.classify("Quanto é 2+2?")).thenReturn(Intent.DESCONHECIDO);
        when(leadExtractor.extract("Quanto é 2+2?", Intent.DESCONHECIDO))
                .thenReturn(new LeadExtraction(null, null, null, null));
        when(ragRetrievalService.retrieve(eq(tenantContext), any(), eq("PORTUGUESE")))
                .thenReturn(List.of(unrelatedChunk));
        when(externalToolRepository.findAll(tenantContext)).thenReturn(List.of());
        when(controlledToolPlanner.executeIfUseful(eq(tenantContext), eq("Quanto é 2+2?"), eq(List.of())))
                .thenReturn(List.of());

        ChatResponse response = service.answer(
                tenantContext,
                new ChatRequest(null, "Quanto é 2+2?", "WEB", "teste", "PORTUGUESE"));

        assertThat(response.answer()).contains("Não encontrei essa informação na base de conhecimento disponível");
        assertThat(response.sources()).isEmpty();
        assertThat(response.fallbackReason()).isEqualTo("SEM_FONTES");
        assertThat(response.handoffRequired()).isTrue();
        verify(chatModelProvider, never()).generate(any());
    }

    private MessageRecord message(UUID conversationId, String role, String content) {
        return new MessageRecord(
                UUID.randomUUID(),
                conversationId,
                role,
                content,
                Intent.DESCONHECIDO.name(),
                OffsetDateTime.now());
    }
}
