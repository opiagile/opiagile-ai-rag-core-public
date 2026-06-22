package com.opiagile.supportai.conversation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {

    private final int historyLimit;

    public ConversationMemoryService(@Value("${conversation.history-limit:8}") int historyLimit) {
        this.historyLimit = Math.max(1, historyLimit);
    }

    public int historyLimit() {
        return historyLimit;
    }

    public String buildRetrievalQuery(List<MessageRecord> recentMessages, String currentMessage) {
        String previousUserMessages = recentMessages.stream()
                .filter(message -> "USER".equals(message.role()))
                .map(MessageRecord::content)
                .collect(Collectors.joining(" "));
        if (previousUserMessages.isBlank()) {
            return currentMessage;
        }
        return previousUserMessages + " " + currentMessage;
    }

    public String summarize(List<MessageRecord> messages) {
        if (messages.isEmpty()) {
            return "Conversa sem mensagens registradas.";
        }
        String lastUserMessage = messages.stream()
                .filter(message -> "USER".equals(message.role()))
                .reduce((first, second) -> second)
                .map(MessageRecord::content)
                .orElse("sem mensagem do usuário");
        long userMessages = messages.stream().filter(message -> "USER".equals(message.role())).count();
        long assistantMessages = messages.stream().filter(message -> "ASSISTANT".equals(message.role())).count();
        return "Conversa com " + userMessages + " mensagens do usuário e " + assistantMessages
                + " respostas da IA. Última mensagem do usuário: " + lastUserMessage;
    }
}
