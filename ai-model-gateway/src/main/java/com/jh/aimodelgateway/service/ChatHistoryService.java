package com.jh.aimodelgateway.service;

import cn.hutool.core.util.ObjectUtil;
import com.jh.aimodelgateway.dto.AiCallMetadata;
import com.jh.aimodelgateway.dto.ChatHistoryResponse;
import com.jh.aimodelgateway.entity.AIConversation;
import com.jh.aimodelgateway.entity.AIMessage;
import com.jh.aimodelgateway.repository.AIConversationService;
import com.jh.aimodelgateway.repository.AIMessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author jinhang
 * @since 2026/8/12 21:38
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatHistoryService {
  private final AIMessageService messageService;
  private final AIConversationService conversationService;
  private final TransactionTemplate transactionTemplate;

  public void saveUserMessage(String conversationId, String content) {

    transactionTemplate.executeWithoutResult(
        status -> {
          AIConversation conversation = conversationService.getByConversationId(conversationId);
          if (ObjectUtil.isNull(conversation)) {
            conversation = new AIConversation();
            conversation.setConversationId(conversationId);
            conversation.setTitle("新对话");
            conversation.setStatus(1);
            conversationService.save(conversation);
          }
          AIMessage message = new AIMessage();
          message.setConversationId(conversationId);
          message.setRole("USER");
          message.setContent(content);

          messageService.save(message);
        });
  }

  public void saveAssistantMessage(
      String conversationId, String content, AiCallMetadata metadata) {
    AIMessage message = new AIMessage();

    message.setConversationId(conversationId);
    message.setRole("ASSISTANT");
    message.setContent(content);
    message.setModel(metadata.model());
    message.setInputTokens(metadata.inputTokens());
    message.setOutputTokens(metadata.outputTokens());
    message.setTotalTokens(metadata.totalTokens());
    message.setFinishReason(metadata.finishReason());
    message.setDurationMs(metadata.durationMs());

    messageService.save(message);
  }

  public List<ChatHistoryResponse> getChatHistory(String conversationId) {
    return messageService.getByConversationId(conversationId).stream()
        .map(
            message ->
                new ChatHistoryResponse(
                    message.getRole(), message.getContent(), message.getCreateTime()))
        .toList();
  }
}
