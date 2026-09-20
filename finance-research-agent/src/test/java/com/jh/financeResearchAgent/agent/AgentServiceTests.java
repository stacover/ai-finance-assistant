package com.jh.financeResearchAgent.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
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
  @ValueSource(ints = {1, 3, 8})
  void modelFailurePreservesPriorResultsAndCountsFailedCall(int failingCall) {
    ChatModel model = mock(ChatModel.class);
    ToolCallingManager manager = mock(ToolCallingManager.class);
    AtomicInteger modelCalls = new AtomicInteger();
    AtomicInteger toolBatches = new AtomicInteger();
    List<String> priorObservations = new ArrayList<>();
    when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
      int call = modelCalls.incrementAndGet();
      if (call > failingCall) {
        throw new AssertionError("Agent must stop after a model failure");
      }
      if (call == failingCall) {
        throw new IllegalStateException("Simulated model connection failure");
      }
      return new ChatResponse(List.of(new Generation(new AssistantMessage(
          "", Map.of(), List.of(toolCall("call_" + call, "半导体"))))));
    });
    when(manager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
        .thenAnswer(invocation -> {
          int batch = toolBatches.incrementAndGet();
          Prompt prompt = invocation.getArgument(0);
          ChatResponse response = invocation.getArgument(1);
          AssistantMessage output = response.getResult().getOutput();
          String observation = "{\"status\":\"SUCCESS\",\"data\":{\"round\":" + batch + "}}";
          priorObservations.add(observation);
          List<Message> history = new ArrayList<>(prompt.getInstructions());
          history.add(output);
          history.add(new ToolResponseMessage(List.of(
              toolResponse(output.getToolCalls().get(0).id(), observation))));
          return ToolExecutionResult.builder().conversationHistory(history).build();
        });

    AgentService service = new AgentService(model, manager, new ToolCallback[0], new ObjectMapper());
    AgentRunResult result = service.run("研究半导体板块");

    assertThat(result.status()).isEqualTo(AgentRunStatus.FAILED);
    assertThat(result.stopReason()).isEqualTo(AgentStopReason.MODEL_ERROR);
    assertThat(result.answer()).isNull();
    assertThat(result.message()).isNotBlank();
    assertThat(result.modelCalls()).isEqualTo(failingCall);
    assertThat(modelCalls.get()).isEqualTo(failingCall);
    assertThat(toolBatches.get()).isEqualTo(failingCall - 1);
    assertThat(result.trace()).isNotNull();
    assertThat(result.trace().getUserQuery()).isEqualTo("研究半导体板块");
    assertThat(result.trace().getSteps()).hasSize(failingCall - 1);
    assertThat(result.trace().getSteps()).extracting(AgentStep::observation)
        .containsExactlyElementsOf(priorObservations);
    assertThat(result.trace().getSteps()).allSatisfy(step ->
        assertThat(step.status()).isEqualTo(StepStatus.SUCCESS));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void eighthModelCallDistinguishesBudgetExhaustionFromFinalAnswer(boolean finalAnswerOnEighth) {
    ChatModel model = mock(ChatModel.class);
    ToolCallingManager manager = mock(ToolCallingManager.class);
    AtomicInteger modelCalls = new AtomicInteger();
    List<String> observations = new ArrayList<>();
    when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
      int round = modelCalls.incrementAndGet();
      if (round > 8) {
        throw new AssertionError("The Agent exceeded its eight-call budget");
      }
      AssistantMessage output = finalAnswerOnEighth && round == 8
          ? new AssistantMessage("最终答复")
          : new AssistantMessage("", Map.of(), List.of(
              toolCall("call_" + round + "_1", "半导体"),
              toolCall("call_" + round + "_2", "新能源")));
      return new ChatResponse(List.of(new Generation(output)));
    });
    when(manager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
        .thenAnswer(invocation -> {
          Prompt prompt = invocation.getArgument(0);
          ChatResponse response = invocation.getArgument(1);
          AssistantMessage output = response.getResult().getOutput();
          List<ToolResponseMessage.ToolResponse> responses = output.getToolCalls().stream()
              .map(call -> {
                String observation = "{\"status\":\"SUCCESS\",\"data\":{\"callId\":\""
                    + call.id() + "\"}}";
                observations.add(observation);
                return toolResponse(call.id(), observation);
              }).toList();
          List<Message> history = new ArrayList<>(prompt.getInstructions());
          history.add(output);
          history.add(new ToolResponseMessage(responses));
          return ToolExecutionResult.builder().conversationHistory(history).build();
        });

    AgentService service = new AgentService(model, manager, new ToolCallback[0], new ObjectMapper());
    AgentRunResult result = service.run("持续比较两个板块");

    assertThat(modelCalls.get()).isEqualTo(8);
    assertThat(result.modelCalls()).isEqualTo(8);
    // Two tools per round: the budget counts model calls, not tool calls.
    assertThat(result.trace().getSteps()).hasSize(finalAnswerOnEighth ? 14 : 16);
    assertThat(result.trace().getSteps()).extracting(AgentStep::observation)
        .containsExactlyElementsOf(observations);
    assertThat(result.trace().getSteps()).allSatisfy(step ->
        assertThat(step.status()).isEqualTo(StepStatus.SUCCESS));
    if (finalAnswerOnEighth) {
      assertThat(result.answer()).isEqualTo("最终答复");
      assertThat(result.stopReason()).isEqualTo(AgentStopReason.FINAL_ANSWER);
      assertThat(result.message()).isNull();
      assertThat(result.status()).isEqualTo(AgentRunStatus.COMPLETED);
    } else {
      assertThat(result.answer()).isNull();
      assertThat(result.stopReason()).isEqualTo(AgentStopReason.MAX_STEPS);
      assertThat(result.message()).isNotBlank();
      assertThat(result.status()).isEqualTo(AgentRunStatus.INCOMPLETE);
    }
  }

  @Test
  void directAnswerReportsOneModelCallAndNormalCompletion() {
    ChatModel model = mock(ChatModel.class);
    ToolCallingManager manager = mock(ToolCallingManager.class);
    when(model.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("直接回答")))));
    ObjectMapper mapper = new ObjectMapper();
    AgentService service = new AgentService(model, manager, new ToolCallback[0], mapper);

    AgentRunResult result = service.run("你好");

    assertThat(result.answer()).isEqualTo("直接回答");
    assertThat(result.trace().getSteps()).isEmpty();
    var response = mapper.valueToTree(result);
    assertThat(response.path("status").asText()).isEqualTo("COMPLETED");
    assertThat(response.path("stopReason").asText()).isEqualTo("FINAL_ANSWER");
    assertThat(response.path("modelCalls").asInt()).isEqualTo(1);
    assertThat(response.path("message").isNull()).isTrue();
  }

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
    var response = new ObjectMapper().valueToTree(result);
    assertThat(response.path("status").asText()).isEqualTo("COMPLETED");
    assertThat(response.path("stopReason").asText()).isEqualTo("FINAL_ANSWER");
    // One call requests tools, and the next call reads their results and answers.
    assertThat(response.path("modelCalls").asInt()).isEqualTo(2);
    assertThat(response.path("message").isNull()).isTrue();
    return result;
  }

  private AssistantMessage.ToolCall toolCall(String id, String sector) {
    return new AssistantMessage.ToolCall(id, "function", "getSectorQuote", "{\"sector\":\"" + sector + "\"}");
  }

  private ToolResponseMessage.ToolResponse toolResponse(String id, String observation) {
    return new ToolResponseMessage.ToolResponse(id, "getSectorQuote", observation);
  }
}
