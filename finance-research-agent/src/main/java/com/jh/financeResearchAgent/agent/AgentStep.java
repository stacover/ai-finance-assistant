package com.jh.financeResearchAgent.agent;

/**
 * @author jinhang
 * @since 2026/9/2 22:09
 */
public record AgentStep(
        String toolName,
        String arguments,
        String result
) {
}
