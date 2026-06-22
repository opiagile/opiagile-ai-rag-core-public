package com.opiagile.supportai.handoff;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.opiagile.supportai.conversation.ConversationMemoryService;
import com.opiagile.supportai.conversation.MessageRepository;
import com.opiagile.supportai.lead.Intent;

@Service
public class HandoffService {

    private final HandoffRepository handoffRepository;
    private final MessageRepository messageRepository;
    private final ConversationMemoryService memoryService;

    public HandoffService(
            HandoffRepository handoffRepository,
            MessageRepository messageRepository,
            ConversationMemoryService memoryService) {
        this.handoffRepository = handoffRepository;
        this.messageRepository = messageRepository;
        this.memoryService = memoryService;
    }

    public HandoffRecord createIfNeeded(UUID conversationId, Intent intent, boolean withoutSources) {
        return handoffRepository.findOpenByConversationId(conversationId)
                .orElseGet(() -> handoffRepository.create(
                        conversationId,
                        reason(intent, withoutSources),
                        memoryService.summarize(messageRepository.findByConversationId(conversationId))));
    }

    public List<HandoffResponse> findAll() {
        return handoffRepository.findAll().stream()
                .map(HandoffResponse::from)
                .toList();
    }

    public HandoffResponse findById(UUID id) {
        return handoffRepository.findById(id)
                .map(HandoffResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Handoff não encontrado: " + id));
    }

    public HandoffResponse updateStatus(UUID id, String status) {
        return HandoffResponse.from(handoffRepository.updateStatus(id, parseStatus(status)));
    }

    public String reason(Intent intent, boolean withoutSources) {
        if (intent == Intent.FALAR_COM_HUMANO) {
            return "Usuário pediu atendimento humano.";
        }
        if (intent == Intent.RECLAMACAO) {
            return "Usuário registrou reclamação ou insatisfação.";
        }
        if (intent == Intent.FORA_DO_ESCOPO) {
            return "Mensagem fora do escopo da base de atendimento.";
        }
        if (withoutSources) {
            return "A base de conhecimento não retornou fontes suficientes.";
        }
        return "Triagem marcou necessidade de revisão humana.";
    }

    private HandoffStatus parseStatus(String status) {
        try {
            return HandoffStatus.valueOf(status.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Status de handoff inválido: " + status);
        }
    }
}
