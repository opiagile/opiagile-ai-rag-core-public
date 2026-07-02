package com.opiagile.supportai.tool;

import java.util.List;

public interface ToolPlanProvider {

    ToolPlanDecision decide(String currentMessage, List<ExternalToolRecord> availableTools);
}
