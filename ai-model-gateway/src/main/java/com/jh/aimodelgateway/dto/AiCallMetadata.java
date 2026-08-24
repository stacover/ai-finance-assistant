package com.jh.aimodelgateway.dto;

/**
 * @author jinhang
 * @since 2026/8/13 20:50
 */
public record AiCallMetadata(
    String model,
    Integer inputTokens,
    Integer outputTokens,
    Integer totalTokens,
    Long durationMs,
    String finishReason) {}
