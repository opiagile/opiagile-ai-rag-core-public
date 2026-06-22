package com.opiagile.supportai.chat;

import java.util.UUID;

public record ChatSourceResponse(
        UUID documentId,
        String filename,
        UUID chunkId,
        double score,
        String excerpt) {
}
