package com.opiagile.supportai.chat;

import java.util.List;

import com.opiagile.supportai.conversation.MessageRecord;
import com.opiagile.supportai.lead.Intent;
import com.opiagile.supportai.rag.RetrievedChunk;

public record ChatPrompt(
        String currentMessage,
        Intent intent,
        String leadStatus,
        String responseLanguage,
        boolean handoffRequired,
        String fallbackReason,
        List<MessageRecord> recentMessages,
        List<RetrievedChunk> sources) {
}
