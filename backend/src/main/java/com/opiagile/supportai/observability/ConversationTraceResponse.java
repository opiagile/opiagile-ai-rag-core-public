package com.opiagile.supportai.observability;

import java.util.List;
import java.util.UUID;

import com.opiagile.supportai.conversation.MessageResponse;
import com.opiagile.supportai.handoff.HandoffResponse;

public record ConversationTraceResponse(
        UUID conversationId,
        List<MessageResponse> messages,
        List<RetrievalTraceResponse> retrievals,
        List<HandoffResponse> handoffs) {
}
