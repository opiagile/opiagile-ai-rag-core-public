package com.opiagile.supportai.chat;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        UUID conversationId,
        @NotBlank(message = "Informe uma mensagem para o atendimento.")
        @Size(max = 1200, message = "A mensagem deve ter no máximo 1200 caracteres.") String message,
        @Size(max = 32, message = "O canal deve ter no máximo 32 caracteres.")
        String channel,
        @Size(max = 128, message = "O identificador de contato deve ter no máximo 128 caracteres.")
        String contactId,
        @Pattern(
                regexp = "(?i)^(EN|ENGLISH|ES|SPANISH|PT|PORTUGUESE|PORTUGUES)$",
                message = "O idioma da resposta deve ser ENGLISH, SPANISH ou PORTUGUESE.")
        String responseLanguage) {
}
