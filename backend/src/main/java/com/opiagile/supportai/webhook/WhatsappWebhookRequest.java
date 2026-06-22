package com.opiagile.supportai.webhook;

import jakarta.validation.constraints.NotBlank;

public record WhatsappWebhookRequest(
        String provider,
        @NotBlank(message = "Informe o telefone de origem.") String from,
        String name,
        @NotBlank(message = "Informe a mensagem recebida.") String message,
        String timestamp) {
}
