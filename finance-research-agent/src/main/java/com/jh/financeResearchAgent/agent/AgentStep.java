package com.jh.financeResearchAgent.agent;

/**
 * Agent 轮次中的一次工具调用记录。
 *
 * @param batchDurationMs 本轮整批工具调用的处理耗时；单个工具的耗时见 observation 中的 ToolResult.durationMs
 * @author jinhang
 * @since 2026/9/2 22:09
 */
public record AgentStep(
        int roundNumber,
        int toolCallIndex,
        String toolName,
        String arguments,
        String observation,
        long batchDurationMs,
        StepStatus status,
        String errorMessage
) {}
