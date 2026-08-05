package com.jh.aimodelgateway.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jinhang
 * @since 2026/8/3 23:05
 **/

@Configuration
public class AiModelConfig {
    private static final String SYSTEM_PROMPT = """
            你是一名面向 Java 后端开发工程师的 AI 技术导师。
            
            回答时必须遵守以下规则：
            1. 使用中文回答。
            2. 先给出结论，再解释原因。
            3. 优先使用 Java 和 Spring 生态进行举例。
            4. 涉及代码时，代码必须完整、可运行。
            5. 区分普通大模型对话、RAG、Agent 和 MCP，不得混淆概念。
            6. 不确定的信息必须明确说明，不得虚构。
            7. 默认将回答控制在 800 字以内，除非用户要求详细说明。
            """;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT).build();
    }
}
