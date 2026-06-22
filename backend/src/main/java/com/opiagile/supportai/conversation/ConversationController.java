package com.opiagile.supportai.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final MessageRepository messageRepository;
    private final ConversationMemoryService memoryService;

    public ConversationController(MessageRepository messageRepository, ConversationMemoryService memoryService) {
        this.messageRepository = messageRepository;
        this.memoryService = memoryService;
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> findMessages(@PathVariable UUID id) {
        return messageRepository.findByConversationId(id).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @GetMapping("/{id}/summary")
    public ConversationSummaryResponse summarize(@PathVariable UUID id) {
        return new ConversationSummaryResponse(id, memoryService.summarize(messageRepository.findByConversationId(id)));
    }
}
