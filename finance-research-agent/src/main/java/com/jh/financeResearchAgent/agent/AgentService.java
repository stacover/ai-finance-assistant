package com.jh.financeResearchAgent.agent;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
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

  public AgentRunResult run(String userQuery) {
    AgentTrace agentTrace = new AgentTrace(userQuery);
    ToolCallingChatOptions chatOptions =
        ToolCallingChatOptions.builder()
            .toolCallbacks(toolCallbacks)
            .internalToolExecutionEnabled(false)
            .build();
    Prompt prompt = buildInitialPrompt(userQuery, chatOptions);
    for (int i = 0; i < MAX_STEPS; i++) {
      ChatResponse chatResponse = chatModel.call(prompt);
      log.info("===== Agent Step {} =====", i + 1);
      log.info("hasToolCalls = {}", chatResponse.hasToolCalls());
      if (!chatResponse.hasToolCalls()) {
        if (chatResponse.getResult() == null) {
          throw new IllegalStateException("LLM return empty response");
        }
        String answer = chatResponse.getResult().getOutput().getText();
        return new AgentRunResult(answer, agentTrace);
      }
      long start = System.currentTimeMillis();
      List<AssistantMessage.ToolCall> toolCalls =
          chatResponse.getResult().getOutput().getToolCalls();
      logToolCalls(i, chatResponse);
      ToolExecutionResult executionResult;
      try {
        executionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
      } catch (Exception e) {
        long duration = System.currentTimeMillis() - start;
        for (int j = 0; j < toolCalls.size(); j++) {
          var toolCall = toolCalls.get(j);
          agentTrace.addStep(
              new AgentStep(
                  i + 1, // roundNumber
                  j + 1, // toolCallIndex
                  toolCall.name(),
                  toolCall.arguments(),
                  null,
                  duration,
                  StepStatus.FAILED,
                  e.getMessage()));
        }
        throw e;
      }
      long duration = System.currentTimeMillis() - start;

      Map<String, String> observations = extractObservations(executionResult);

      for (int j = 0; j < toolCalls.size(); j++) {
        var toolCall = toolCalls.get(j);
        String observation = observations.get(toolCall.name());
        agentTrace.addStep(
            new AgentStep(
                i + 1, // roundNumber
                j + 1, // toolCallIndex
                toolCall.name(),
                toolCall.arguments(),
                observation,
                duration,
                StepStatus.SUCCESS,
                null));
      }
      prompt = new Prompt(executionResult.conversationHistory(), chatOptions);
    }
    throw new IllegalStateException("Agent exceeded max steps: " + MAX_STEPS);
  }

  private Map<String, String> extractObservations(ToolExecutionResult executionResult) {

    var history = executionResult.conversationHistory();

    if (history.isEmpty()) {
      return Map.of();
    }

    for (int i = history.size() - 1; i >= 0; i--) {

      var message = history.get(i);

      if (message instanceof ToolResponseMessage toolResponseMessage) {

        return toolResponseMessage.getResponses().stream()
            .collect(
                Collectors.toMap(
                    ToolResponseMessage.ToolResponse::name,
                    ToolResponseMessage.ToolResponse::responseData,
                    (oldValue, newValue) -> newValue));
      }
    }

    return Map.of();
  }

  @Nonnull
  private static Prompt buildInitialPrompt(String userQuery, ToolCallingChatOptions chatOptions) {
    List<Message> messages =
        List.of(
            new SystemMessage(
                """
            你是一个金融市场研究 Agent。

                    你的任务不是简单回答用户问题，
                    而是根据已有信息逐步调查并形成有依据的分析。

                    规则：

                    1. 涉及行情、资金、成分股等事实数据时，
                       必须优先通过工具获取，不允许自行编造。

                    2. 每次获得工具结果后，
                       判断当前信息是否足以回答用户问题。

                    3. 如果信息不足，
                       应继续选择最有价值的工具获取数据。

                    4. 不要为了调用工具而调用工具，
                       已有信息足够时应停止调查。

                    5. 必须区分：
                       - 已获取事实
                       - 基于事实的推断
                       - 当前无法确认的信息

                    6. 最终回答必须基于实际工具结果。
            """),
            new UserMessage(userQuery));

    return new Prompt(messages, chatOptions);
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
