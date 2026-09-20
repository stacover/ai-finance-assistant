package com.jh.financeResearchAgent.agent;

/** 整次 Agent 运行的结束状态，与单个工具的 StepStatus 分开。 */
public enum AgentRunStatus {
  /** 模型已返回最终答复；不代表答复质量已经通过业务评估。 */
  COMPLETED,
  /** 系统主动停止，尚未形成最终答复，例如模型调用轮次耗尽。 */
  INCOMPLETE,
  /** 执行出现错误而中断，仍应保留此前获得的工具结果。 */
  FAILED
}
