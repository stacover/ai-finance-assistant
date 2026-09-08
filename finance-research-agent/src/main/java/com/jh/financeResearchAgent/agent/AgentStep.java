package com.jh.financeResearchAgent.agent;

/**
 * @author jinhang
 * @since 2026/9/2 22:09
 */
public record AgentStep(
        int roundNumber,
        int toolCallIndex,
        String toolName,
        String arguments,
        String observation,
        long durationMs,
        StepStatus status,
        String errorMessage
) {}
