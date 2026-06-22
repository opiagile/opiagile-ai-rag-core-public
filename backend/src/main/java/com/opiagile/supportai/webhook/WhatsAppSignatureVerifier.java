package com.opiagile.supportai.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppSignatureVerifier {

    private final WhatsAppProperties properties;

    public WhatsAppSignatureVerifier(WhatsAppProperties properties) {
        this.properties = properties;
    }

    public boolean verify(byte[] rawBody, String signatureHeader) {
        if (!properties.isSignatureRequired()) {
            return true;
        }
        if (!properties.hasAppSecret() || signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expected = "sha256=" + hmacSha256(rawBody, properties.getAppSecret());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    String hmacSha256(byte[] rawBody, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(rawBody));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível validar assinatura WhatsApp.", exception);
        }
    }
}
