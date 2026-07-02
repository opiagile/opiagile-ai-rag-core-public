package com.opiagile.supportai.tool;

public record ToolPlanDecision(
        ToolPlanAction action,
        double confidence,
        String reason) {

    public static ToolPlanDecision none(String reason) {
        return new ToolPlanDecision(ToolPlanAction.NONE, 0.0, reason);
    }
}
