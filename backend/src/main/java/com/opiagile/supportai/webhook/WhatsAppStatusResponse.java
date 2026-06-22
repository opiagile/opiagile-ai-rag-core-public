package com.opiagile.supportai.webhook;

public record WhatsAppStatusResponse(
        String provider,
        boolean dryRun,
        boolean sendEnabled,
        boolean signatureRequired,
        boolean publicBaseUrlConfigured,
        boolean phoneNumberIdConfigured,
        boolean accessTokenConfigured,
        boolean appSecretConfigured,
        boolean verifyTokenConfigured,
        int allowedTestNumbersCount,
        int rateLimitPerMinute,
        boolean blockUnauthorized) {

    public static WhatsAppStatusResponse from(WhatsAppProperties properties, WhatsAppTesterAllowlistService allowlistService) {
        return new WhatsAppStatusResponse(
                properties.getProvider(),
                properties.isDryRun(),
                properties.isSendEnabled(),
                properties.isSignatureRequired(),
                properties.hasPublicBaseUrl(),
                properties.hasPhoneNumberId(),
                properties.hasAccessToken(),
                properties.hasAppSecret(),
                properties.hasVerifyToken(),
                allowlistService.allowedCount(),
                properties.getRateLimitPerMinute(),
                properties.isBlockUnauthorized());
    }
}
