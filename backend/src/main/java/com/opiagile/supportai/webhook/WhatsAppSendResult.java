package com.opiagile.supportai.webhook;

public record WhatsAppSendResult(
        String status,
        boolean sent,
        boolean dryRun,
        String blockedReason,
        String providerMessageId) {

    public static WhatsAppSendResult sent(String providerMessageId) {
        return new WhatsAppSendResult("SENT", true, false, null, providerMessageId);
    }

    public static WhatsAppSendResult dryRunResult() {
        return new WhatsAppSendResult("DRY_RUN", false, true, null, null);
    }

    public static WhatsAppSendResult skipped(String reason) {
        return new WhatsAppSendResult("SKIPPED", false, false, reason, null);
    }

    public static WhatsAppSendResult failed(String reason) {
        return new WhatsAppSendResult("FAILED", false, false, reason, null);
    }
}
