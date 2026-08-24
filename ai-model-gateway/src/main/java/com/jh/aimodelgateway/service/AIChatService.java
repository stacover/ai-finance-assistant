package com.jh.aimodelgateway.service;

import com.jh.aimodelgateway.dto.*;
import com.jh.aimodelgateway.exception.AiErrorCode;
import com.jh.aimodelgateway.exception.AiModelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author jinhang
 * @since 2025/12/23 15:11
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIChatService {

  private final ChatClient chatClient;
  private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;
  private final ChatMemory chatMemory;
  private final ChatHistoryService chatHistoryService;

  public AIChatResponse chat(ChatRequest request) {

    try {

      var response = chatClient.prompt().user(request.message()).call().chatResponse();

      return new AIChatResponse(response.getResult().getOutput().getText());

    } catch (Exception exception) {

      throw convertModelException(exception);
    }
  }

  public Flux<String> stream(ChatRequest request) {
    return chatClient.prompt().user(request.message()).stream().content();
  }

  public TechnicalAnswer structuredChat(ChatRequest request) {
    return chatClient
        .prompt()
        .user(
            user ->
                user.text(
                        """
                请回答下面的 Java 或 AI 技术问题。

                问题：
                {question}

                要求：
                1. conclusion 给出简洁结论；
                2. explanation 解释核心原理；
                3. keyPoints 返回 3 至 5 个关键点；
                4. 不确定的信息必须明确说明。
                """)
                    .param("question", request.message()))
        .call()
        .entity(TechnicalAnswer.class);
  }

  public AIChatResponse chatWithMemory(ConversationChatRequest request) {
    chatHistoryService.saveUserMessage(request.conversationId(), request.message());

    long start = System.currentTimeMillis();
    ChatResponse chatResponse = callModel(request);
    long end = System.currentTimeMillis();
    String content = chatResponse.getResult().getOutput().getText();
    Usage usage = chatResponse.getMetadata().getUsage();
    AiCallMetadata metadata =
        new AiCallMetadata(
            chatResponse.getMetadata().getModel(),
            usage.getPromptTokens(),
            usage.getCompletionTokens(),
            usage.getTotalTokens(),
            end - start,
            chatResponse.getResult().getMetadata().getFinishReason());
    chatHistoryService.saveAssistantMessage(request.conversationId(), content, metadata);
    return new AIChatResponse(content);
  }

  private ChatResponse callModel(ConversationChatRequest request) {

    try {

      return chatClient
          .prompt()
          .advisors(
              advisor ->
                  advisor
                      .advisors(messageChatMemoryAdvisor)
                      .param(ChatMemory.CONVERSATION_ID, request.conversationId()))
          .user(request.message())
          .call()
          .chatResponse();

    } catch (Exception exception) {

      throw convertModelException(exception);
    }
  }

  private AiModelException convertModelException(Exception exception) {

    Throwable rootCause = getRootCause(exception);

    if (rootCause instanceof java.net.SocketTimeoutException) {
      return new AiModelException(AiErrorCode.MODEL_TIMEOUT, exception);
    }

    if (exception instanceof org.springframework.web.client.ResourceAccessException) {
      return new AiModelException(AiErrorCode.MODEL_UNAVAILABLE, exception);
    }

    return new AiModelException(AiErrorCode.MODEL_REQUEST_FAILED, exception);
  }

  private Throwable getRootCause(Throwable throwable) {

    Throwable cause = throwable;

    while (cause.getCause() != null) {
      cause = cause.getCause();
    }

    return cause;
  }

  public void clearMemory(String conversationId) {
    chatMemory.clear(conversationId);
  }
}
