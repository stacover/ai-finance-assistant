package com.jh.aiknowledgeservice.dto;

/**
 * @author jinhang
 * @since 2026/9/1 22:06
 */
public record DocumentSearchResult(
    int chunkIndex, int pageNUmber, String text, double similarity) {}
