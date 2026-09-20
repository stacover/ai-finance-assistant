package com.jh.financeResearchAgent.agent;

/**
 * Agent 的最终运行结果，包括未完成或异常退出时已获得的信息。
 *
 * @param answer 模型的最终答复；尚未形成最终答复时为 null
 * @param trace 已记录的工具执行过程与原始结果，非正常结束时也应保留
 * @param status 整次运行的结束状态，不等同于某一个工具的成功或失败
 * @param stopReason 停止原因
 * @param modelCalls 已发起的 chatModel.call 次数，包含抛出异常的调用，不是研究完成百分比
 * @param message 面向调用方的停止说明；正常完成时可为 null
 * @author jinhang
 * @since 2026/9/8 21:51
 */
public record AgentRunResult(
    String answer,
    AgentTrace trace,
    AgentRunStatus status,
    AgentStopReason stopReason,
    int modelCalls,
    String message) {}
