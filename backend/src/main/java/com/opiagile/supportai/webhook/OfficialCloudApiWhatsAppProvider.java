package com.opiagile.supportai.webhook;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "META_CLOUD")
public class OfficialCloudApiWhatsAppProvider implements WhatsAppProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OfficialCloudApiWhatsAppProvider.class);

    private final WhatsAppProperties properties;
    private final WhatsAppTesterAllowlistService allowlistService;
    private final RestClient restClient;

    public OfficialCloudApiWhatsAppProvider(
            WhatsAppProperties properties,
            WhatsAppTesterAllowlistService allowlistService,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.allowlistService = allowlistService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = restClientBuilder
                .baseUrl("https://graph.facebook.com")
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String providerName() {
        return "META_CLOUD";
    }

    @Override
    public void sendMessage(String to, String message) {
        sendText(to, message);
    }

    public WhatsAppSendResult sendText(String to, String message) {
        if (!properties.isMetaCloud()) {
            return WhatsAppSendResult.skipped("PROVIDER_NAO_META_CLOUD");
        }
        if (properties.isDryRun()) {
            LOGGER.info("Dry-run WhatsApp Meta para {}. Envio real não executado.", PhoneNumberMasker.mask(to));
            return WhatsAppSendResult.dryRunResult();
        }
        if (!properties.isSendEnabled()) {
            return WhatsAppSendResult.skipped("ENVIO_DESABILITADO");
        }
        if (!properties.hasAccessToken()) {
            return WhatsAppSendResult.skipped("TOKEN_AUSENTE");
        }
        if (!properties.hasPhoneNumberId()) {
            return WhatsAppSendResult.skipped("PHONE_NUMBER_ID_AUSENTE");
        }
        if (!allowlistService.isAllowed(to)) {
            return WhatsAppSendResult.skipped("NUMERO_FORA_DA_ALLOWLIST");
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", properties.getGraphApiVersion(), properties.getPhoneNumberId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload(to, message))
                    .retrieve()
                    .body(JsonNode.class);
            String messageId = response == null ? null : response.path("messages").path(0).path("id").asText(null);
            return WhatsAppSendResult.sent(messageId);
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha segura ao enviar WhatsApp Meta para {}: {}", PhoneNumberMasker.mask(to), exception.getMessage());
            return WhatsAppSendResult.failed("ERRO_META_API");
        }
    }

    private Map<String, Object> payload(String to, String message) {
        Map<String, Object> text = new LinkedHashMap<>();
        text.put("preview_url", false);
        text.put("body", message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", PhoneNumberMasker.normalize(to));
        body.put("type", "text");
        body.put("text", text);
        return body;
    }
}
