package com.opiagile.supportai.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

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
import com.opiagile.supportai.lead.LeadStatus;
import com.opiagile.supportai.rag.RagRetrievalService;
import com.opiagile.supportai.rag.RetrievalLogRepository;
import com.opiagile.supportai.rag.RetrievedChunk;

@Service
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemoryService memoryService;
    private final LeadRepository leadRepository;
    private final IntentClassifier intentClassifier;
    private final LeadExtractor leadExtractor;
    private final HandoffService handoffService;
    private final RagRetrievalService ragRetrievalService;
    private final RetrievalLogRepository retrievalLogRepository;
    private final ChatModelProvider chatModelProvider;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ConversationMemoryService memoryService,
            LeadRepository leadRepository,
            IntentClassifier intentClassifier,
            LeadExtractor leadExtractor,
            HandoffService handoffService,
            RagRetrievalService ragRetrievalService,
            RetrievalLogRepository retrievalLogRepository,
            ChatModelProvider chatModelProvider) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memoryService = memoryService;
        this.leadRepository = leadRepository;
        this.intentClassifier = intentClassifier;
        this.leadExtractor = leadExtractor;
        this.handoffService = handoffService;
        this.ragRetrievalService = ragRetrievalService;
        this.retrievalLogRepository = retrievalLogRepository;
        this.chatModelProvider = chatModelProvider;
    }

    public ChatResponse answer(ChatRequest request) {
        Instant startedAt = Instant.now();
        UUID conversationId = conversationRepository.ensureConversation(
                request.conversationId(),
                normalizeChannel(request.channel()),
                request.contactId());

        Intent intent = intentClassifier.classify(request.message());
        LeadExtraction extraction = leadExtractor.extract(request.message(), intent);
        List<MessageRecord> recentMessages = messageRepository.findRecent(conversationId, memoryService.historyLimit());
        messageRepository.save(conversationId, "USER", request.message(), intent.name());

        String retrievalQuery = memoryService.buildRetrievalQuery(recentMessages, request.message());
        List<RetrievedChunk> retrievedChunks = ragRetrievalService.retrieve(retrievalQuery);
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();

        List<ChatSourceResponse> sources = retrievedChunks.stream()
                .map(chunk -> new ChatSourceResponse(
                        chunk.documentId(),
                        chunk.filename(),
                        chunk.chunkId(),
                        chunk.score(),
                        chunk.excerpt()))
                .toList();

        boolean handoffRequired = requiresHuman(intent, sources.isEmpty());
        String fallbackReason = fallbackReason(intent, sources.isEmpty());
        String leadStatus = leadStatus(intent, handoffRequired).name();
        ChatGenerationResult generation = chatModelProvider.generate(new ChatPrompt(
                request.message(),
                intent,
                leadStatus,
                handoffRequired,
                fallbackReason,
                recentMessages,
                retrievedChunks));
        String effectiveFallbackReason = generation.fallbackReason() == null
                ? fallbackReason
                : generation.fallbackReason();
        retrievalLogRepository.save(
                conversationId,
                retrievalQuery,
                retrievedChunks,
                (int) latencyMs,
                intent.name(),
                handoffRequired,
                effectiveFallbackReason,
                retrievalProvider(generation, retrievedChunks),
                generation.responseMode(),
                generation.llmProvider(),
                generation.model(),
                (int) generation.llmLatencyMs(),
                generation.promptTokens(),
                generation.completionTokens(),
                generation.totalTokens());
        String answer = generation.answer();
        messageRepository.save(conversationId, "ASSISTANT", answer, intent.name());
        leadRepository.upsert(
                conversationId,
                leadStatus,
                memoryService.summarize(messageRepository.findRecent(conversationId, memoryService.historyLimit())),
                extraction);
        if (handoffRequired) {
            handoffService.createIfNeeded(conversationId, intent, sources.isEmpty());
        }

        return new ChatResponse(
                conversationId,
                answer,
                intent.name(),
                sources,
                handoffRequired,
                leadStatus,
                latencyMs,
                generation.responseMode(),
                generation.llmProvider(),
                generation.model(),
                effectiveFallbackReason,
                generation.llmLatencyMs(),
                generation.promptTokens(),
                generation.completionTokens(),
                generation.totalTokens());
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "WEB";
        }
        return channel.trim().toUpperCase();
    }

    private boolean requiresHuman(Intent intent, boolean withoutSources) {
        return withoutSources
                || intent == Intent.FALAR_COM_HUMANO
                || intent == Intent.RECLAMACAO
                || intent == Intent.FORA_DO_ESCOPO;
    }

    private String fallbackReason(Intent intent, boolean withoutSources) {
        if (withoutSources) {
            return "SEM_FONTES";
        }
        if (intent == Intent.FALAR_COM_HUMANO) {
            return "USUARIO_PEDIU_HUMANO";
        }
        if (intent == Intent.RECLAMACAO) {
            return "RECLAMACAO";
        }
        if (intent == Intent.FORA_DO_ESCOPO) {
            return "FORA_DO_ESCOPO";
        }
        return null;
    }

    private LeadStatus leadStatus(Intent intent, boolean handoffRequired) {
        if (handoffRequired) {
            return LeadStatus.NEEDS_HUMAN;
        }
        if (intent == Intent.AGENDAR || intent == Intent.COMERCIAL || intent == Intent.REMARCAR) {
            return LeadStatus.QUALIFIED;
        }
        return LeadStatus.QUALIFYING;
    }

    private String retrievalProvider(ChatGenerationResult generation, List<RetrievedChunk> chunks) {
        String retrievalProvider = "local-text";
        if (chunks != null && !chunks.isEmpty()) {
            retrievalProvider = chunks.getFirst().retrievalProvider();
        }
        if ("LLM".equals(generation.responseMode())) {
            return retrievalProvider + "+" + generation.llmProvider();
        }
        return retrievalProvider;
    }
}
