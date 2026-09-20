package com.jh.financeResearchAgent.agent;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author jinhang
 * @since 2026/9/2 21:56
 */
public class AgentContext {

  @Getter
  private final String userQuery;

  private final List<AgentStep> steps = new ArrayList<>();

  public AgentContext(String userQuery) {
    this.userQuery = userQuery;
  }

  public void addStep(AgentStep step) {
    this.steps.add(step);
  }

    public List<AgentStep> getSteps() {
    return Collections.unmodifiableList(steps);
  }
}
