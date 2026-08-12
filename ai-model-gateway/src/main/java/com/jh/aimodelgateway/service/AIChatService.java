package com.jh.aimodelgateway.service;

import com.jh.aimodelgateway.dto.ChatRequest;
import com.jh.aimodelgateway.dto.ChatResponse;
import com.jh.aimodelgateway.dto.ConversationChatRequest;
import com.jh.aimodelgateway.dto.TechnicalAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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

  public ChatResponse chat(ChatRequest request) {
    String content = chatClient.prompt().user(request.message()).call().content();
    return new ChatResponse(content);
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

  public ChatResponse chatWithMemory(ConversationChatRequest request) {
    chatHistoryService.saveUserMessage(request.conversationId(), request.message());

    long start = System.currentTimeMillis();
    String content =
        chatClient
            .prompt()
            .advisors(
                advisorSpec ->
                    advisorSpec
                        .advisors(messageChatMemoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, request.conversationId()))
            .user(request.message())
            .call()
            .content();
    long end = System.currentTimeMillis();
    chatHistoryService.saveAssistantMessage(
        request.conversationId(), content, "deepseek-v4-flash", end - start);
    return new ChatResponse(content);
  }

  public void clearMemory(String conversationId) {
    chatMemory.clear(conversationId);
  }
}
