package com.opiagile.supportai.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class WhatsAppMetaPayloadParserTest {

    private final WhatsAppMetaPayloadParser parser = new WhatsAppMetaPayloadParser(new ObjectMapper());

    @Test
    void deveExtrairMensagemTextoDoPayloadMeta() {
        List<WhatsAppInboundMessage> messages = parser.parse(textPayload().getBytes(StandardCharsets.UTF_8));

        assertThat(messages).hasSize(1);
        WhatsAppInboundMessage message = messages.getFirst();
        assertThat(message.isTextMessage()).isTrue();
        assertThat(message.from()).isEqualTo("5511999998888");
        assertThat(message.contactName()).isEqualTo("João");
        assertThat(message.text()).isEqualTo("Quero agendar uma consulta");
        assertThat(message.phoneNumberId()).isEqualTo("12345");
    }

    @Test
    void deveIgnorarStatusUpdate() {
        List<WhatsAppInboundMessage> messages = parser.parse(statusPayload().getBytes(StandardCharsets.UTF_8));

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().eventType()).isEqualTo("status");
        assertThat(messages.getFirst().isTextMessage()).isFalse();
    }

    @Test
    void deveIgnorarMidiaNestaSprint() {
        List<WhatsAppInboundMessage> messages = parser.parse(imagePayload().getBytes(StandardCharsets.UTF_8));

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().eventType()).isEqualTo("message_ignored");
        assertThat(messages.getFirst().messageType()).isEqualTo("image");
    }

    static String textPayload() {
        return """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "metadata": {"phone_number_id": "12345"},
                        "contacts": [{"profile": {"name": "João"}, "wa_id": "5511999998888"}],
                        "messages": [{
                          "from": "5511999998888",
                          "id": "wamid.TESTE",
                          "timestamp": "1717000000",
                          "type": "text",
                          "text": {"body": "Quero agendar uma consulta"}
                        }]
                      }
                    }]
                  }]
                }
                """;
    }

    private String statusPayload() {
        return """
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"12345"},"statuses":[{"id":"wamid.TESTE","status":"sent"}]}}]}]}
                """;
    }

    private String imagePayload() {
        return """
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"12345"},"messages":[{"from":"5511999998888","id":"wamid.TESTE","type":"image"}]}}]}]}
                """;
    }
}
