package com.opiagile.supportai.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(WhatsAppProvider.class)
public class FallbackWhatsAppProvider implements WhatsAppProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackWhatsAppProvider.class);

    @Override
    public String providerName() {
        return "MOCK_FALLBACK";
    }

    @Override
    public void sendMessage(String to, String message) {
        LOGGER.warn("Provider WhatsApp não reconhecido. Envio real bloqueado para {}.", PhoneNumberMasker.mask(to));
    }
}
