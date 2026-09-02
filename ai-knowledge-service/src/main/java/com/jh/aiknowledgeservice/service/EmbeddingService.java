package com.jh.aiknowledgeservice.service;

import com.jh.aiknowledgeservice.dto.EmbeddingResponse;
import com.jh.aiknowledgeservice.dto.SimilarityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * @author jinhang
 * @since 2026/8/31 22:10
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {
 private final EmbeddingModel embeddingModel;

    public EmbeddingResponse embed(String text) {
       float[] vector = embeddingModel.embed(text);
        return new EmbeddingResponse(text,vector.length,vector);
    }

    public SimilarityResponse similarity(String text,String text1) {
        float[] vector = embeddingModel.embed(text);
        float[] vector1 = embeddingModel.embed(text1);
        double similarity = cosineSimilarity(vector, vector1);
        return new SimilarityResponse(text,text1,similarity);
    }

    public double cosineSimilarity(float[] vector, float[] vector1) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vector.length; i++) {
            dotProduct += vector[i] * vector1[i];
            normA += vector[i] * vector[i];
            normB += vector1[i] * vector1[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
