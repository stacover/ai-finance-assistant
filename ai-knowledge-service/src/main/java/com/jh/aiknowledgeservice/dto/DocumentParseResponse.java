package com.jh.aiknowledgeservice.dto;

import java.util.Map;

/**
 * @author jinhang
 * @since 2026/8/26 21:53
 */
public record DocumentParseResponse(String id, String fileName, int textLength, String text, Map<String,Object> metaData) {}
