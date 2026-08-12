package com.jh.aimodelgateway.controller;

import com.jh.aimodelgateway.dto.ChatRequest;
import com.jh.aimodelgateway.dto.ChatResponse;
import com.jh.aimodelgateway.dto.ConversationChatRequest;
import com.jh.aimodelgateway.dto.TechnicalAnswer;
import com.jh.aimodelgateway.service.AIChatService;
import jakarta.validation.constraints.NotBlank;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author jinhang
 * @since 2026/8/3 22:28
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class AIChatController {
  private final AIChatService aiChatService;

  @PostMapping
  public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
    return aiChatService.chat(request);
  }

  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest request) {
    return aiChatService.stream(request)
        .filter(content -> content != null && !content.isEmpty())
        .map(content -> ServerSentEvent.<String>builder().event("message").data(content).build())
        .concatWith(
            Flux.just(ServerSentEvent.<String>builder().event("done").data("[DONE]").build()))
        .onErrorResume(
            exception ->
                Flux.just(
                    ServerSentEvent.<String>builder().event("error").data("模型调用失败，请稍后重试").build()));
  }

  @PostMapping("/structured")
  public TechnicalAnswer structuredChat(@Valid @RequestBody ChatRequest request) {
    return aiChatService.structuredChat(request);
  }

  @PostMapping("/memory")
  public ChatResponse memoryChat(@Valid @RequestBody ConversationChatRequest request) {
    return aiChatService.chatWithMemory(request);
  }

  @DeleteMapping("/clearMemory/{conversationId}")
  public ResponseEntity<Void> clearMemory(@PathVariable @NotBlank String conversationId) {
    aiChatService.clearMemory(conversationId);
    return ResponseEntity.noContent().build();
  }
}
