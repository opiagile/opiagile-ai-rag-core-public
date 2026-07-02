package com.opiagile.supportai.webhook;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class WhatsAppMetaPayloadParser {

    private final ObjectMapper objectMapper;

    public WhatsAppMetaPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<WhatsAppInboundMessage> parse(byte[] rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            List<WhatsAppInboundMessage> events = new ArrayList<>();
            for (JsonNode entry : iterable(root.path("entry"))) {
                for (JsonNode change : iterable(entry.path("changes"))) {
                    JsonNode value = change.path("value");
                    String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                    if (value.path("statuses").isArray() && value.path("messages").isMissingNode()) {
                        events.add(new WhatsAppInboundMessage("status", phoneNumberId, null, null, null, null, null, null));
                    }
                    for (JsonNode message : iterable(value.path("messages"))) {
                        String type = message.path("type").asText("unknown");
                        String from = message.path("from").asText(null);
                        String name = contactName(value, from);
                        if ("text".equals(type)) {
                            events.add(new WhatsAppInboundMessage(
                                    "message_text",
                                    phoneNumberId,
                                    from,
                                    name,
                                    message.path("id").asText(null),
                                    message.path("timestamp").asText(null),
                                    type,
                                    message.path("text").path("body").asText(null)));
                        } else {
                            events.add(new WhatsAppInboundMessage(
                                    "message_ignored",
                                    phoneNumberId,
                                    from,
                                    name,
                                    message.path("id").asText(null),
                                    message.path("timestamp").asText(null),
                                    type,
                                    null));
                        }
                    }
                }
            }
            return events;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Payload Meta WhatsApp inválido.", exception);
        }
    }

    private String contactName(JsonNode value, String from) {
        for (JsonNode contact : iterable(value.path("contacts"))) {
            String waId = contact.path("wa_id").asText("");
            if (from == null || from.equals(waId)) {
                return contact.path("profile").path("name").asText(null);
            }
        }
        return null;
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return node;
    }
}
