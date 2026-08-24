package com.jh.aimodelgateway.dto;

import java.time.LocalDateTime;

/**
 * @author jinhang
 * @since 2026/8/24 22:05
 */
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
}
