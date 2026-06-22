package com.opiagile.supportai.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "MOCK", matchIfMissing = true)
public class MockWhatsAppProvider implements WhatsAppProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockWhatsAppProvider.class);

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public void sendMessage(String to, String message) {
        LOGGER.info("Mensagem WhatsApp simulada para {}: {}", to, message);
    }
}
