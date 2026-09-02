package com.jh.financeResearchAgent.config;

import com.jh.financeResearchAgent.tool.market.MarketTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jinhang
 * @since 2026/9/2 22:30
 */
@Configuration
public class AgentToolConfiguration {
    @Bean
    public ToolCallback[] agentToolCallbacks(MarketTools marketTools) {
        return ToolCallbacks.from(marketTools);
    }
}
