package com.jh.financeResearchAgent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public AgentRunResult run(String userQuery) {
    AgentTrace agentTrace = new AgentTrace(userQuery);
    ToolCallingChatOptions chatOptions =
        ToolCallingChatOptions.builder()
            .toolCallbacks(toolCallbacks)
            .internalToolExecutionEnabled(false)
            .build();
    Prompt prompt = buildInitialPrompt(userQuery, chatOptions);
    for (int i = 0; i < MAX_STEPS; i++) {
      ChatResponse chatResponse;
      try {
        chatResponse = chatModel.call(prompt);
      } catch (Exception e) {
        log.error("LLM call failed", e);
        return new AgentRunResult(
            null,
            agentTrace,
            AgentRunStatus.FAILED,
            AgentStopReason.MODEL_ERROR,
            i + 1,
            "模型调用失败，已保留本次执行记录");
      }
      log.info("===== Agent Round {} =====", i + 1);
      log.info("hasToolCalls = {}", chatResponse.hasToolCalls());
      if (!chatResponse.hasToolCalls()) {
        if (chatResponse.getResult() == null) {
          throw new IllegalStateException("LLM return empty response");
        }
        String answer = chatResponse.getResult().getOutput().getText();
        return new AgentRunResult(
            answer,
            agentTrace,
            AgentRunStatus.COMPLETED,
            AgentStopReason.FINAL_ANSWER,
            i + 1,
            null);
      }
      List<AssistantMessage.ToolCall> toolCalls =
          chatResponse.getResult().getOutput().getToolCalls();
      logToolCalls(i + 1, chatResponse);
      ToolExecutionResult executionResult;
      long batchStartTime = System.currentTimeMillis();
      try {
        executionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
      } catch (Exception e) {
        long batchDurationMs = System.currentTimeMillis() - batchStartTime;
        for (int j = 0; j < toolCalls.size(); j++) {
          var toolCall = toolCalls.get(j);
          agentTrace.addStep(
              new AgentStep(
                  i + 1, // roundNumber
                  j + 1, // toolCallIndex
                  toolCall.name(),
                  toolCall.arguments(),
                  null,
                  batchDurationMs,
                  StepStatus.FAILED,
                  e.getMessage()));
        }
        throw e;
      }
      long batchDurationMs = System.currentTimeMillis() - batchStartTime;

      Map<String, String> observations = extractObservations(executionResult);

      for (int j = 0; j < toolCalls.size(); j++) {
        var toolCall = toolCalls.get(j);
        String observation = observations.get(toolCall.id());
        ObservationOutcome outcome = parseObservation(observation);
        agentTrace.addStep(
            new AgentStep(
                i + 1, // roundNumber
                j + 1, // toolCallIndex
                toolCall.name(),
                toolCall.arguments(),
                observation,
                batchDurationMs,
                outcome.status(),
                outcome.errorMessage()));
      }
      prompt = new Prompt(executionResult.conversationHistory(), chatOptions);
    }
    return new AgentRunResult(
        null,
        agentTrace,
        AgentRunStatus.INCOMPLETE,
        AgentStopReason.MAX_STEPS,
        MAX_STEPS,
        "已达到模型调用次数上限，本次研究尚未完成，已获取的工具结果保留在 Trace 中");
  }

  // 解析只影响 Trace 状态；原始 observation 仍保留供排查和后续模型调用使用。
  private ObservationOutcome parseObservation(String observation) {
    if (observation == null || observation.isBlank()) {
      return new ObservationOutcome(StepStatus.FAILED, "工具返回结果缺失或为空");
    }

    final JsonNode result;
    try {
      result = objectMapper.readTree(observation);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse tool observation as JSON");
      return new ObservationOutcome(StepStatus.FAILED, "工具返回结果不是合法 JSON");
    }

    if (result == null || !result.isObject()) {
      return new ObservationOutcome(StepStatus.FAILED, "工具返回结果必须是 JSON 对象");
    }
    JsonNode status = result.path("status");
    if (!status.isTextual() || status.asText().isBlank()) {
      return new ObservationOutcome(StepStatus.FAILED, "工具返回结果缺少有效的 status 字段");
    }

    return switch (status.asText()) {
      case "SUCCESS" -> new ObservationOutcome(StepStatus.SUCCESS, null);
      case "FAILURE" -> {
        JsonNode message = result.path("message");
        String errorMessage =
            message.isTextual() && !message.asText().isBlank()
                ? message.asText()
                : "工具执行失败，未提供错误说明";
        yield new ObservationOutcome(StepStatus.FAILED, errorMessage);
      }
      default -> new ObservationOutcome(StepStatus.FAILED, "未知工具状态：" + status.asText());
    };
  }

  private record ObservationOutcome(StepStatus status, String errorMessage) {}

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
                    ToolResponseMessage.ToolResponse::id,
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

                工具失败规则：
                1. Tool 返回 FAILED 时，不得将失败结果视为业务数据。

                2. 不要因为 retriable=true 而自行重复调用完全相同的 Tool。
                   网络超时等技术性重试由系统 Runtime 负责。

                3. 如果存在其他能够获取等价数据的 Tool，
                   可以选择替代工具。

                4. 如果缺失数据不是完成任务的必要条件，
                   应基于已成功获取的数据进行降级分析。

                5. 缺失关键数据时，必须明确说明分析限制。
                """),
            new UserMessage(userQuery));

    return new Prompt(messages, chatOptions);
  }

  private void logToolCalls(int roundNumber, ChatResponse response) {

    if (response.getResult() == null) {
      return;
    }

    AssistantMessage assistantMessage = response.getResult().getOutput();

    List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls();
    for (int i = 0; i < toolCalls.size(); i++) {
      AssistantMessage.ToolCall toolCall = toolCalls.get(i);

      log.info(
          "Agent Round={}, ToolCallIndex={}, Tool={}, Arguments={}",
          roundNumber,
          i + 1,
          toolCall.name(),
          toolCall.arguments());
    }
  }
}
