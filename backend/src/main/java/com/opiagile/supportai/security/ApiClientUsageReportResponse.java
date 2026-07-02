package com.opiagile.supportai.security;

import java.util.List;

public record ApiClientUsageReportResponse(
        List<ApiClientUsageSummaryResponse> summaries,
        List<ApiClientUsageLogEntryResponse> recentEvents) {
}
