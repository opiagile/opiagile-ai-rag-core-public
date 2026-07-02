package com.opiagile.supportai.developer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class DeveloperAccessKeyDeliveryService {

    private static final int TOKEN_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final DeveloperAccessKeyDeliveryRepository repository;
    private final DeveloperAccessEmailProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeveloperAccessKeyDeliveryService(
            DeveloperAccessKeyDeliveryRepository repository,
            DeveloperAccessEmailProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    DeveloperAccessKeyDeliveryLink createLink(
            UUID requestId,
            UUID apiClientId,
            String apiKey,
            String keyPrefix,
            String tenantSlug,
            String workspaceSlug,
            Set<String> scopes,
            int rateLimitPerMinute,
            OffsetDateTime sandboxExpiresAt,
            String retentionNotice) {
        String token = randomToken();
        OffsetDateTime deliveryExpiresAt = deliveryExpiresAt(sandboxExpiresAt);
        UUID id = repository.save(
                requestId,
                apiClientId,
                tokenHash(token),
                encrypt(apiKey, token),
                keyPrefix,
                tenantSlug,
                workspaceSlug,
                scopes,
                rateLimitPerMinute,
                sandboxExpiresAt,
                deliveryExpiresAt,
                retentionNotice);
        return new DeveloperAccessKeyDeliveryLink(id, token, accessUrl(token), deliveryExpiresAt);
    }

    boolean available(String token) {
        return token != null && repository.available(tokenHash(token));
    }

    Optional<RevealedDeveloperAccessKey> reveal(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return repository.consume(tokenHash(token))
                    .map(delivery -> new RevealedDeveloperAccessKey(
                            decrypt(delivery.encryptedApiKey(), token),
                            delivery.keyPrefix(),
                            delivery.tenantSlug(),
                            delivery.workspaceSlug(),
                            delivery.scopes(),
                            delivery.rateLimitPerMinute(),
                            delivery.sandboxExpiresAt(),
                            delivery.deliveryExpiresAt(),
                            delivery.retentionNotice()));
        } catch (IllegalStateException exception) {
            return Optional.empty();
        }
    }

    private OffsetDateTime deliveryExpiresAt(OffsetDateTime sandboxExpiresAt) {
        OffsetDateTime fallback = OffsetDateTime.now().plusHours(properties.keyDeliveryExpiresInHours());
        if (sandboxExpiresAt == null) {
            return fallback;
        }
        return sandboxExpiresAt.isBefore(fallback) ? sandboxExpiresAt : fallback;
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String accessUrl(String token) {
        return properties.publicBaseUrl() + "/developers/access-key/" + token;
    }

    private String encrypt(String value, String token) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(token), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Falha ao preparar entrega segura da API key.", exception);
        }
    }

    private String decrypt(String encryptedValue, String token) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encryptedValue);
            if (payload.length <= IV_BYTES) {
                throw new IllegalStateException("Payload de entrega inválido.");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            System.arraycopy(payload, IV_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(token), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Falha ao revelar API key.", exception);
        }
    }

    private SecretKeySpec key(String token) {
        byte[] digest = sha256(("opiagile-key-delivery:" + token).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    private String tokenHash(String token) {
        byte[] digest = sha256(token.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    public record RevealedDeveloperAccessKey(
            String apiKey,
            String keyPrefix,
            String tenantSlug,
            String workspaceSlug,
            Set<String> scopes,
            int rateLimitPerMinute,
            OffsetDateTime sandboxExpiresAt,
            OffsetDateTime deliveryExpiresAt,
            String retentionNotice) {
    }
}
