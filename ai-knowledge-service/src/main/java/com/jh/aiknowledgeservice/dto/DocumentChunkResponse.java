package com.jh.aiknowledgeservice.dto;

import java.util.Map;

/**
 * @author jinhang
 * @since 2026/8/26 22:21
 */
public record DocumentChunkResponse(
        int index,
        String id,
        int textLength,
        String text,
        Map<String, Object> metadata
) {
}
