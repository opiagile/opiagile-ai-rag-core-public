package com.opiagile.supportai.webhook;

import org.springframework.stereotype.Service;

@Service
public class DefaultWhatsAppOutboundService {

    private final WhatsAppProperties properties;
    private final WhatsAppProvider whatsAppProvider;

    public DefaultWhatsAppOutboundService(WhatsAppProperties properties, WhatsAppProvider whatsAppProvider) {
        this.properties = properties;
        this.whatsAppProvider = whatsAppProvider;
    }

    public WhatsAppSendResult send(String to, String message) {
        if (!properties.isMetaCloud()) {
            whatsAppProvider.sendMessage(to, message);
            return WhatsAppSendResult.dryRunResult();
        }
        if (whatsAppProvider instanceof OfficialCloudApiWhatsAppProvider officialProvider) {
            return officialProvider.sendText(to, message);
        }
        return WhatsAppSendResult.skipped("PROVIDER_META_NAO_CONFIGURADO");
    }
}
