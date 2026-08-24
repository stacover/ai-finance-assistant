package com.jh.aimodelgateway.dto;

import java.time.LocalDateTime;

/**
 * @author jinhang
 * @since 2026/8/12 22:06
 */
public record ChatHistoryResponse(String role, String content, LocalDateTime createTime) {}
