package com.jh.aiknowledgeservice.controller;

import com.jh.aiknowledgeservice.dto.EmbeddingResponse;
import com.jh.aiknowledgeservice.dto.SimilarityResponse;
import com.jh.aiknowledgeservice.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinhang
 * @since 2026/8/31 22:13
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmbeddingController {

  private final EmbeddingService embeddingService;

  @GetMapping("/embeddings")
  public EmbeddingResponse embed(@RequestParam String text) {
    return embeddingService.embed(text);
  }

  @GetMapping("/similarity")
  public SimilarityResponse embed(@RequestParam String text, @RequestParam String text1) {
    return embeddingService.similarity(text, text1);
  }
}
