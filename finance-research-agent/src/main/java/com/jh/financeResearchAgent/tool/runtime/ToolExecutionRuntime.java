package com.jh.financeResearchAgent.tool.runtime;

import com.jh.financeResearchAgent.tool.ToolResult;

import java.util.concurrent.Callable;

/**
 * @author jinhang
 * @since 2026/9/15 21:56
 */
public interface ToolExecutionRuntime {

  <T> ToolResult<T> invoke(String toolName, ToolExecutionPolicy policy, Callable<T> action);
}
