package com.opiagile.supportai.chat;

public interface ChatModelProvider {

    ChatGenerationResult generate(ChatPrompt prompt);

    String providerName();
}
