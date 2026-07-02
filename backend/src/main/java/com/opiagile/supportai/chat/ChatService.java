package com.opiagile.supportai.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
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
import com.opiagile.supportai.rag.MultilingualQueryExpander;
import com.opiagile.supportai.rag.RagRetrievalService;
import com.opiagile.supportai.rag.RetrievalLogRepository;
import com.opiagile.supportai.rag.RetrievedChunk;
import com.opiagile.supportai.rag.TextSimilarityScorer;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tool.ExternalToolRecord;
import com.opiagile.supportai.tool.ExternalToolRepository;
import com.opiagile.supportai.tool.ControlledToolPlanner;
import com.opiagile.supportai.tool.ToolExecutionResult;

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
    private final MultilingualQueryExpander multilingualQueryExpander;
    private final TextSimilarityScorer textSimilarityScorer;
    private final ExternalToolRepository externalToolRepository;
    private final ControlledToolPlanner controlledToolPlanner;
    private final double currentMessageMinSourceScore;

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
            ChatModelProvider chatModelProvider,
            MultilingualQueryExpander multilingualQueryExpander,
            TextSimilarityScorer textSimilarityScorer,
            ExternalToolRepository externalToolRepository,
            ControlledToolPlanner controlledToolPlanner,
            @Value("${chat.current-message-min-source-score:0.15}") double currentMessageMinSourceScore) {
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
        this.multilingualQueryExpander = multilingualQueryExpander;
        this.textSimilarityScorer = textSimilarityScorer;
        this.externalToolRepository = externalToolRepository;
        this.controlledToolPlanner = controlledToolPlanner;
        this.currentMessageMinSourceScore = Math.max(0.0, currentMessageMinSourceScore);
    }

    public ChatResponse answer(TenantContext tenantContext, ChatRequest request) {
        Instant startedAt = Instant.now();
        UUID conversationId = conversationRepository.ensureConversation(
                tenantContext,
                request.conversationId(),
                normalizeChannel(request.channel()),
                request.contactId());

        Intent intent = intentClassifier.classify(request.message());
        LeadExtraction extraction = leadExtractor.extract(request.message(), intent);
        List<MessageRecord> recentMessages = messageRepository.findRecent(conversationId, memoryService.historyLimit());
        messageRepository.save(conversationId, "USER", request.message(), intent.name());

        String responseLanguage = normalizeResponseLanguage(request.responseLanguage());
        String retrievalQuery = multilingualQueryExpander.expand(
                memoryService.buildRetrievalQuery(recentMessages, request.message()),
                responseLanguage);
        List<RetrievedChunk> retrievedChunks = ragRetrievalService.retrieve(tenantContext, retrievalQuery, responseLanguage);
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();

        String currentMessageQuery = multilingualQueryExpander.expand(request.message(), responseLanguage);
        boolean hasSourceForCurrentMessage = hasSourceForCurrentMessage(currentMessageQuery, retrievedChunks);

        List<ExternalToolRecord> availableTools = externalToolRepository.findAll(tenantContext);
        List<ToolExecutionResult> toolResults = controlledToolPlanner.executeIfUseful(
                tenantContext,
                request.message(),
                availableTools);
        boolean withoutReliableContext = (!hasSourceForCurrentMessage
                && noSuccessfulToolResult(toolResults)
                && !isConversationContinuation(request.message(), recentMessages));
        List<RetrievedChunk> answerChunks = withoutReliableContext ? List.of() : retrievedChunks;

        List<ChatSourceResponse> sources = answerChunks.stream()
                .map(chunk -> new ChatSourceResponse(
                        chunk.documentId(),
                        chunk.filename(),
                        chunk.chunkId(),
                        chunk.score(),
                        chunk.excerpt()))
                .toList();

        boolean handoffRequired = requiresHuman(intent, withoutReliableContext);
        String fallbackReason = fallbackReason(intent, withoutReliableContext);
        String leadStatus = leadStatus(intent, handoffRequired).name();
        ChatGenerationResult generation = generateAnswer(
                request,
                intent,
                leadStatus,
                responseLanguage,
                handoffRequired,
                fallbackReason,
                recentMessages,
                answerChunks,
                availableTools,
                toolResults,
                withoutReliableContext);
        String effectiveFallbackReason = generation.fallbackReason() == null
                ? fallbackReason
                : generation.fallbackReason();
        retrievalLogRepository.save(
                tenantContext,
                conversationId,
                retrievalQuery,
                answerChunks,
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

    private ChatGenerationResult generateAnswer(
            ChatRequest request,
            Intent intent,
            String leadStatus,
            String responseLanguage,
            boolean handoffRequired,
            String fallbackReason,
            List<MessageRecord> recentMessages,
            List<RetrievedChunk> answerChunks,
            List<ExternalToolRecord> availableTools,
            List<ToolExecutionResult> toolResults,
            boolean withoutReliableContext) {
        if (withoutReliableContext && !allowsOperationalFallback(intent)) {
            return ChatGenerationResult.demo(noReliableContextAnswer(responseLanguage), fallbackReason, 0);
        }
        return chatModelProvider.generate(new ChatPrompt(
                request.message(),
                intent,
                leadStatus,
                responseLanguage,
                handoffRequired,
                fallbackReason,
                recentMessages,
                answerChunks,
                availableTools,
                toolResults));
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "WEB";
        }
        return channel.trim().toUpperCase();
    }

    private String normalizeResponseLanguage(String responseLanguage) {
        if (responseLanguage == null || responseLanguage.isBlank()) {
            return "PORTUGUESE";
        }
        return switch (responseLanguage.trim().toUpperCase()) {
            case "EN", "ENGLISH" -> "ENGLISH";
            case "ES", "SPANISH" -> "SPANISH";
            case "PT", "PORTUGUESE", "PORTUGUES" -> "PORTUGUESE";
            default -> "PORTUGUESE";
        };
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

    private boolean hasSourceForCurrentMessage(String currentMessageQuery, List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        return chunks.stream()
                .anyMatch(chunk -> textSimilarityScorer.score(currentMessageQuery, chunk.content()) >= currentMessageMinSourceScore);
    }

    private boolean isConversationContinuation(String message, List<MessageRecord> recentMessages) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return false;
        }
        String normalized = normalizeForContinuation(message);
        return switch (normalized) {
            case "sim", "ok", "certo", "pode seguir", "seguir", "confirmo", "confirmado",
                    "esta de acordo", "estou de acordo", "fico no aguardo", "sem mais",
                    "nao tenho mais nada", "obrigado", "obrigada" -> true;
            default -> false;
        };
    }

    private String normalizeForContinuation(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean allowsOperationalFallback(Intent intent) {
        return intent == Intent.FALAR_COM_HUMANO
                || intent == Intent.RECLAMACAO
                || intent == Intent.FORA_DO_ESCOPO;
    }

    private String noReliableContextAnswer(String responseLanguage) {
        if ("ENGLISH".equals(responseLanguage)) {
            return "I did not find this information in the available knowledge base, so I cannot answer it safely from the provided sources.";
        }
        if ("SPANISH".equals(responseLanguage)) {
            return "No encontré esta información en la base de conocimiento disponible, así que no puedo responderla con seguridad a partir de las fuentes proporcionadas.";
        }
        return "Não encontrei essa informação na base de conhecimento disponível, então não consigo responder com segurança a partir das fontes fornecidas.";
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

    private boolean noSuccessfulToolResult(List<ToolExecutionResult> toolResults) {
        return toolResults == null || toolResults.stream().noneMatch(result -> "SUCCESS".equals(result.status()));
    }
}
