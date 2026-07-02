package com.opiagile.supportai.tool;

import java.util.List;
import java.util.Map;

public record ToolExecutionResult(
        String toolSlug,
        String purpose,
        String status,
        String sqlPreview,
        int rowCount,
        boolean truncated,
        int latencyMs,
        List<String> columns,
        List<Map<String, Object>> rows,
        String errorMessage) {

    public static ToolExecutionResult success(String purpose, String sqlPreview, SqlToolExecutionResponse response) {
        return new ToolExecutionResult(
                response.toolSlug(),
                purpose,
                "SUCCESS",
                sqlPreview,
                response.rowCount(),
                response.truncated(),
                response.latencyMs(),
                response.columns(),
                response.rows(),
                null);
    }

    public static ToolExecutionResult error(String toolSlug, String purpose, String sqlPreview, String errorMessage) {
        return new ToolExecutionResult(
                toolSlug,
                purpose,
                "ERROR",
                sqlPreview,
                0,
                false,
                0,
                List.of(),
                List.of(),
                errorMessage);
    }
}
