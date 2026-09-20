package com.jh.financeResearchAgent.tool.runtime;

import com.jh.financeResearchAgent.tool.ToolResult;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author jinhang
 * @since 2026/9/15 21:56
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultToolExecutionRuntime implements ToolExecutionRuntime {

  private final ExecutorService executorService;

  @Override
  public <T> ToolResult<T> invoke(
      String toolName, ToolExecutionPolicy policy, Callable<T> action) {

    // 整个 Tool 调用生命周期开始时间
    long invokeStartTime = System.currentTimeMillis();

    ToolResult<T> lastFailure = null;

    for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {

      // 单次 attempt 开始时间，只用于日志
      long attemptStartTime = System.currentTimeMillis();

      Future<T> future = executorService.submit(action);

      try {

        T data = future.get(policy.timeoutPerAttempt().toMillis(), TimeUnit.MILLISECONDS);

        long attemptDurationMs = System.currentTimeMillis() - attemptStartTime;

        long totalDurationMs = System.currentTimeMillis() - invokeStartTime;

        log.info(
            "Tool success. tool={}, attempt={}/{}, " + "attemptDurationMs={}, totalDurationMs={}",
            toolName,
            attempt,
            policy.maxAttempts(),
            attemptDurationMs,
            totalDurationMs);

        return ToolResult.success(data, attempt, totalDurationMs);

      } catch (TimeoutException e) {

        future.cancel(true);

        lastFailure = classifyFailure(e);

      } catch (ExecutionException e) {

        Throwable cause = e.getCause() != null ? e.getCause() : e;

        lastFailure = classifyFailure(cause);

      } catch (InterruptedException e) {

        future.cancel(true);

        Thread.currentThread().interrupt();

        long totalDurationMs = System.currentTimeMillis() - invokeStartTime;

        return ToolResult.failure("TOOL_INTERRUPTED", "工具执行被中断", false, attempt, totalDurationMs);
      }

      long attemptDurationMs = System.currentTimeMillis() - attemptStartTime;

      log.warn(
          "Tool failed. tool={}, attempt={}/{}, "
              + "attemptDurationMs={}, errorCode={}, retriable={}",
          toolName,
          attempt,
          policy.maxAttempts(),
          attemptDurationMs,
          lastFailure.errorCode(),
          lastFailure.retriable());

      // 业务上明确不可重试
      if (!lastFailure.retriable()) {

        long totalDurationMs = System.currentTimeMillis() - invokeStartTime;

        return ToolResult.failure(
            lastFailure.errorCode(), lastFailure.message(), false, attempt, totalDurationMs);
      }

      // 已经是最后一次尝试
      if (attempt >= policy.maxAttempts()) {
        break;
      }

      // Retry Backoff
      boolean backoffCompleted = sleepBackoff(policy.retryBackoff());

      if (!backoffCompleted) {

        long totalDurationMs = System.currentTimeMillis() - invokeStartTime;

        return ToolResult.failure("TOOL_INTERRUPTED", "工具重试等待被中断", false, attempt, totalDurationMs);
      }
    }

    long totalDurationMs = System.currentTimeMillis() - invokeStartTime;

    if (lastFailure == null) {

      return ToolResult.failure(
          "TOOL_EXECUTION_ERROR", "工具执行失败", false, policy.maxAttempts(), totalDurationMs);
    }

    /*
     * 所有 Runtime Retry 已经耗尽。
     *
     * 此时 retriable 必须改为 false，
     * 不再让 LLM 根据 retriable=true
     * 自己重新调用同一个 Tool。
     */
    return ToolResult.failure(
        lastFailure.errorCode(),
        lastFailure.message(),
        false,
        policy.maxAttempts(),
        totalDurationMs);
  }

  private <T> ToolResult<T> classifyFailure(Throwable throwable) {

    if (throwable instanceof TimeoutException || throwable instanceof SocketTimeoutException) {

      return ToolResult.failure("TOOL_TIMEOUT", "工具调用超时", true);
    }

    if (throwable instanceof ConnectException) {

      return ToolResult.failure("TOOL_CONNECTION_ERROR", "工具数据源连接失败", true);
    }

    if (throwable instanceof IllegalArgumentException) {

      return ToolResult.failure("TOOL_INVALID_ARGUMENT", throwable.getMessage(), false);
    }

    return ToolResult.failure("TOOL_EXECUTION_ERROR", "工具执行失败", false);
  }

  private boolean sleepBackoff(Duration duration) {

    if (duration.isZero()) {
      return true;
    }

    try {

      Thread.sleep(duration.toMillis());

      return true;

    } catch (InterruptedException e) {

      Thread.currentThread().interrupt();

      return false;
    }
  }
}
