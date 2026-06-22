package com.opiagile.supportai.webhook;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "EVOLUTION_API")
public class EvolutionApiWhatsAppProvider implements WhatsAppProvider {

    @Override
    public String providerName() {
        return "EVOLUTION_API";
    }

    @Override
    public void sendMessage(String to, String message) {
        throw new UnsupportedOperationException("Evolution API está preparada, mas ainda não foi configurada nesta fase.");
    }
}
