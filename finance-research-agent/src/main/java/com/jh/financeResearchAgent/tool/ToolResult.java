package com.jh.financeResearchAgent.tool;

/**
 * @author jinhang
 * @since 2026/9/15 21:06
 */
public record ToolResult<T>(
    ToolStatus status,
    T data,
    String errorCode,
    String message,
    boolean retriable,
    int attempts,
    long durationMs) {

  public static <T> ToolResult<T> success(T data, int attempts, long durationMs) {

    return new ToolResult<>(ToolStatus.SUCCESS, data, null, null, false, attempts, durationMs);
  }

  public static <T> ToolResult<T> failure(
      String errorCode, String message, boolean retriable, int attempts, long durationMs) {

    return new ToolResult<>(
        ToolStatus.FAILURE, null, errorCode, message, retriable, attempts, durationMs);
  }

  /*
   * Runtime 内部进行异常分类时使用。
   * 此时还不知道最终 attempts 和 duration。
   */
  public static <T> ToolResult<T> failure(String errorCode, String message, boolean retriable) {

    return new ToolResult<>(ToolStatus.FAILURE, null, errorCode, message, retriable, 0, 0);
  }
}
