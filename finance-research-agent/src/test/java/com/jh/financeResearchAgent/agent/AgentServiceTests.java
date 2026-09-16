package com.jh.financeResearchAgent.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

class AgentServiceTests {

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"status\":\"SUCCESS\",\"message\":null}",
      "{\"status\":\"SUCCESS\"}",
      "{\"status\":\"SUCCESS\",\"message\":\"informational text\"}"
  })
  void successfulToolDoesNotCarryAnError(String observation) {
    AgentStep step = runWithObservation(observation);
    assertThat(step.status()).isEqualTo(StepStatus.SUCCESS);
    assertThat(step.errorMessage()).isNull();
    assertThat(step.observation()).isEqualTo(observation);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
      "   ", "null", "[]", "42", "{}", "{broken",
      "{\"status\":null}", "{\"status\":\"UNKNOWN\"}",
      "{\"status\":\"FAILURE\"}",
      "{\"status\":\"FAILURE\",\"message\":null}",
      "{\"status\":\"FAILURE\",\"message\":\"   \"}"
  })
  void invalidOrFailedToolResultIsRecordedWithoutAbortingRun(String observation) {
    AgentStep step = runWithObservation(observation);
    assertThat(step.status()).isEqualTo(StepStatus.FAILED);
    assertThat(step.errorMessage()).isNotBlank().isNotEqualTo("null");
    assertThat(step.observation()).isEqualTo(observation);
  }

  @Test
  void sameNamedToolsKeepTheirOwnResultsEvenWhenResponsesAreReversed() {
    String failure = "{\"status\":\"FAILURE\",\"message\":\"工具调用超时\"}";
    String success = "{\"status\":\"SUCCESS\",\"data\":{\"sector\":\"新能源\"}}";
    AgentRunResult result = run(
        List.of(toolCall("call_1", "半导体"), toolCall("call_2", "新能源")),
        List.of(toolResponse("call_2", success), toolResponse("call_1", failure)));

    assertThat(result.trace().getSteps()).hasSize(2);
    AgentStep first = result.trace().getSteps().get(0);
    assertThat(first.status()).isEqualTo(StepStatus.FAILED);
    assertThat(first.errorMessage()).isEqualTo("工具调用超时");
    assertThat(first.observation()).isEqualTo(failure);
    AgentStep second = result.trace().getSteps().get(1);
    assertThat(second.status()).isEqualTo(StepStatus.SUCCESS);
    assertThat(second.errorMessage()).isNull();
    assertThat(second.observation()).isEqualTo(success);
  }

  private AgentStep runWithObservation(String observation) {
    // Missing response for this ID exercises the null observation path.
    List<ToolResponseMessage.ToolResponse> responses = observation == null
        ? List.of() : List.of(toolResponse("call_1", observation));
    AgentRunResult result = run(List.of(toolCall("call_1", "半导体")), responses);
    assertThat(result.trace().getSteps()).hasSize(1);
    return result.trace().getSteps().get(0);
  }

  private AgentRunResult run(
      List<AssistantMessage.ToolCall> calls,
      List<ToolResponseMessage.ToolResponse> responses) {
    ChatModel model = mock(ChatModel.class);
    ToolCallingManager manager = mock(ToolCallingManager.class);
    AssistantMessage toolRequest = new AssistantMessage("", Map.of(), calls);
    ChatResponse first = new ChatResponse(List.of(new Generation(toolRequest)));
    ChatResponse last = new ChatResponse(List.of(new Generation(new AssistantMessage("分析完成"))));
    when(model.call(any(Prompt.class))).thenReturn(first, last);
    when(manager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
        .thenReturn(ToolExecutionResult.builder()
            .conversationHistory(List.of(toolRequest, new ToolResponseMessage(responses)))
            .build());

    AgentService service = new AgentService(model, manager, new ToolCallback[0], new ObjectMapper());
    AgentRunResult result = service.run("比较两个板块");
    assertThat(result.answer()).isEqualTo("分析完成");
    return result;
  }

  private AssistantMessage.ToolCall toolCall(String id, String sector) {
    return new AssistantMessage.ToolCall(id, "function", "getSectorQuote", "{\"sector\":\"" + sector + "\"}");
  }

  private ToolResponseMessage.ToolResponse toolResponse(String id, String observation) {
    return new ToolResponseMessage.ToolResponse(id, "getSectorQuote", observation);
  }
}
