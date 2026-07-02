package com.opiagile.supportai.tool;

import java.util.List;

public record ExternalToolResponse(
        String slug,
        String name,
        String type,
        String status,
        String description,
        List<String> allowedTables,
        int defaultLimit,
        int maxLimit) {

    static ExternalToolResponse from(ExternalToolRecord tool) {
        return new ExternalToolResponse(
                tool.slug(),
                tool.name(),
                tool.type(),
                tool.status(),
                tool.description(),
                tool.allowedTables(),
                tool.defaultLimit(),
                tool.maxLimit());
    }
}
