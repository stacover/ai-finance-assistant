package com.jh.financeResearchAgent.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author jinhang
 * @since 2026/9/8 21:46
 */

@Data
public class AgentTrace {
    private final String traceId;
    private final String userQuery;
    private final long startTime;
    private final List<AgentStep> steps = new ArrayList<>();

    public AgentTrace(String userQuery) {
        this.traceId =
                UUID.randomUUID().toString();

        this.userQuery = userQuery;

        this.startTime =
                System.currentTimeMillis();

    }

    public void addStep(AgentStep agentStep) {
        this.steps.add(agentStep);
    }
}
