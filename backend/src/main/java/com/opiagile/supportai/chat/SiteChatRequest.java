package com.opiagile.supportai.chat;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SiteChatRequest(
        UUID conversationId,
        @NotBlank(message = "Informe uma mensagem para o atendimento.")
        @Size(max = 1000, message = "A mensagem deve ter no máximo 1000 caracteres.") String message,
        @Size(max = 128, message = "O identificador de visitante deve ter no máximo 128 caracteres.")
        String visitorId,
        @Pattern(
                regexp = "(?i)^(EN|ENGLISH|ES|SPANISH|PT|PORTUGUESE|PORTUGUES)$",
                message = "O idioma da resposta deve ser ENGLISH, SPANISH ou PORTUGUESE.")
        String responseLanguage) {
}
