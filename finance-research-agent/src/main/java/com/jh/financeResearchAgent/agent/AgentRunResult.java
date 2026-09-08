package com.jh.financeResearchAgent.agent;

/**
 * @author jinhang
 * @since 2026/9/8 21:51
 */
public record AgentRunResult(
        String answer,
        AgentTrace trace
) {
}
