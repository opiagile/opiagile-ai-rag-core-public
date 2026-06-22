package com.opiagile.supportai.observability;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.conversation.MessageRepository;
import com.opiagile.supportai.conversation.MessageResponse;
import com.opiagile.supportai.handoff.HandoffResponse;
import com.opiagile.supportai.handoff.HandoffRepository;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final MessageRepository messageRepository;
    private final HandoffRepository handoffRepository;
    private final ObservabilityRepository observabilityRepository;

    public ObservabilityController(
            MessageRepository messageRepository,
            HandoffRepository handoffRepository,
            ObservabilityRepository observabilityRepository) {
        this.messageRepository = messageRepository;
        this.handoffRepository = handoffRepository;
        this.observabilityRepository = observabilityRepository;
    }

    @GetMapping("/conversations/{id}/trace")
    public ConversationTraceResponse trace(@PathVariable UUID id) {
        return new ConversationTraceResponse(
                id,
                messageRepository.findByConversationId(id).stream().map(MessageResponse::from).toList(),
                observabilityRepository.findRetrievals(id),
                handoffRepository.findAll().stream()
                        .filter(handoff -> handoff.conversationId().equals(id))
                        .map(HandoffResponse::from)
                        .toList());
    }
}
