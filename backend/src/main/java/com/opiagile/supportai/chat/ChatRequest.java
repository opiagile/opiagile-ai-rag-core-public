package com.opiagile.supportai.chat;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        UUID conversationId,
        @NotBlank(message = "Informe uma mensagem para o atendimento.") String message,
        String channel,
        String contactId) {
}
