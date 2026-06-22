package com.opiagile.supportai.admin;

public record DemoResetResponse(
        int whatsappEvents,
        int retrievalLogs,
        int handoffs,
        int leads,
        int messages,
        int conversations,
        int chunks,
        int documents) {
}
