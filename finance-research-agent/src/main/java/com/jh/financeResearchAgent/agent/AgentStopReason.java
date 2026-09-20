package com.jh.financeResearchAgent.agent;

/** 本次运行停止的具体原因；与概括性的 AgentRunStatus 配合使用。 */
public enum AgentStopReason {
  FINAL_ANSWER,
  /** 达到模型调用次数上限，不是工具调用次数上限。 */
  MAX_STEPS,
  MODEL_ERROR
}
