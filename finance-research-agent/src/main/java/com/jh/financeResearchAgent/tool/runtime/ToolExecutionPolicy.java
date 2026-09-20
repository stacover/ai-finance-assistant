package com.jh.financeResearchAgent.tool.runtime;

import java.time.Duration;

/**
 * @author jinhang
 * @since 2026/9/15 21:43
 */
public record ToolExecutionPolicy(
    int maxAttempts, Duration timeoutPerAttempt, Duration retryBackoff) {
  public ToolExecutionPolicy {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >= 1");
    }
    if (timeoutPerAttempt == null
        || timeoutPerAttempt.isZero()
        || timeoutPerAttempt.isNegative()) {
      throw new IllegalArgumentException("timeoutPerAttempt must be positive");
    }
    if (retryBackoff == null || retryBackoff.isZero() || retryBackoff.isNegative()) {
      throw new IllegalArgumentException("retryBackoff must be positive");
    }
  }
}
