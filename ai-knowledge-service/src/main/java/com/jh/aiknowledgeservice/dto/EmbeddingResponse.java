package com.jh.aiknowledgeservice.dto;

/**
 * @author jinhang
 * @since 2026/8/31 22:13
 */
public record EmbeddingResponse(String text,int dimension,float[] vector) {}
