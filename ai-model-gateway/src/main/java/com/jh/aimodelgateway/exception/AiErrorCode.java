package com.jh.aimodelgateway.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author jinhang
 * @since 2026/8/24 22:06
 */
@Getter
@RequiredArgsConstructor
public enum AiErrorCode {
  MODEL_TIMEOUT("AI_MODEL_TIMEOUT", "模型响应超时，请稍后重试"),

  MODEL_UNAVAILABLE("AI_MODEL_UNAVAILABLE", "模型服务暂时不可用，请稍后重试"),

  MODEL_REQUEST_FAILED("AI_MODEL_REQUEST_FAILED", "模型调用失败，请稍后重试"),

  STRUCTURED_OUTPUT_ERROR("AI_STRUCTURED_OUTPUT_ERROR", "模型返回的数据格式不正确"),

  INVALID_REQUEST("INVALID_REQUEST", "请求参数不正确");

  private final String code;

  private final String message;
}
