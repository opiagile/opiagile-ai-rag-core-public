package com.opiagile.supportai.tool;

import java.util.List;
import java.util.Map;

public record SqlToolExecutionResponse(
        String toolSlug,
        int rowCount,
        boolean truncated,
        int latencyMs,
        List<String> columns,
        List<Map<String, Object>> rows) {
}
