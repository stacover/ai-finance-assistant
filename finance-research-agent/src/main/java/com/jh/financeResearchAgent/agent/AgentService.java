package com.jh.financeResearchAgent.agent;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

/**
 * @author jinhang
 * @since 2026/9/2 22:01
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {
  private static final int MAX_STEPS = 8;
  private final ChatModel chatModel;
  private final ToolCallingManager toolCallingManager;
  private final ToolCallback[] toolCallbacks;

  public String run(String userQuery) {
    ToolCallingChatOptions chatOptions =
        ToolCallingChatOptions.builder()
            .toolCallbacks(toolCallbacks)
            .internalToolExecutionEnabled(false)
            .build();
    List<Message> messages =
        List.of(
            new SystemMessage(
                """
                    你是一个金融市场研究 Agent。

                    你需要根据用户的问题自主判断是否需要调用工具。

                    要求：
                    1. 涉及实时或市场行情数据时，优先使用工具。
                    2. 不允许编造工具能够查询的数据。
                    3. 获取工具结果后再进行分析。
                    4. 最终给出清晰、简洁、有依据的结论。
                    """),
            new UserMessage(userQuery));

    Prompt prompt = new Prompt(messages, chatOptions);
    for (int i = 0; i < MAX_STEPS; i++) {
      ChatResponse chatResponse = chatModel.call(prompt);
      log.info("===== Agent Step {} =====", i + 1);
      log.info("hasToolCalls = {}", chatResponse.hasToolCalls());
      if (!chatResponse.hasToolCalls()) {
        if (chatResponse.getResult() == null) {
          throw new IllegalStateException("LLM return empty response");
        }
        return chatResponse.getResult().getOutput().getText();
      }
      logToolCalls(i, chatResponse);
      ToolExecutionResult toolCalls = toolCallingManager.executeToolCalls(prompt, chatResponse);
      if (toolCalls.returnDirect()) {
        return toolCalls
            .conversationHistory()
            .get(toolCalls.conversationHistory().size() - 1)
            .getText();
      }
      prompt = new Prompt(toolCalls.conversationHistory(), chatOptions);
    }
    throw new IllegalStateException("Agent exceeded max steps: " + MAX_STEPS);
  }

  private void logToolCalls(int step, ChatResponse response) {

    if (response.getResult() == null) {
      return;
    }

    AssistantMessage assistantMessage = response.getResult().getOutput();

    for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {

      log.info(
          """
              Agent Step: {}
              Tool:{}
              Arguments: {}
              """,
          step + 1,
          toolCall.name(),
          toolCall.arguments());
    }
  }
}
