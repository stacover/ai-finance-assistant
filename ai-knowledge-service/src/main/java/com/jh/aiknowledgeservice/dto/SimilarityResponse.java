package com.jh.aiknowledgeservice.dto;

/**
 * @author jinhang
 * @since 2026/9/1 21:40
 */
public record SimilarityResponse(String text1, String text2, Double similarity) {}
