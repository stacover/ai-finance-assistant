package com.jh.financeResearchAgent.controller;

import com.jh.financeResearchAgent.agent.AgentService;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinhang
 * @since 2026/9/2 22:47
 */
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

  private final AgentService agentService;

  @PostMapping("/chat")
  public String chat(@RequestBody AgentRequest request) {
    return agentService.run(request.message());
  }

  public record AgentRequest(String message) {}
}
