package com.jh.aimodelgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * AI 模型动态配置。
 *
 * <p>从 Nacos 配置中心读取并支持热刷新（@RefreshScope），
 * 当 Nacos 上的 ai-model-gateway.yaml 变更后，调用 refresh 端点即可生效。</p>
 *
 * @author jinhang
 * @since 2026/8/12
 */
@RefreshScope
@Component
public class AiModelDynamicConfig {

  /** DeepSeek API Key（来自 Nacos 配置中心，替代本地环境变量） */
  @Value("${spring.ai.openai.api-key:}")
  private String apiKey;

  /** 模型 base-url */
  @Value("${spring.ai.openai.base-url:https://api.deepseek.com}")
  private String baseUrl;

  /** 默认模型名 */
  @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
  private String model;

  /** 采样温度 */
  @Value("${spring.ai.openai.chat.options.temperature:0.7}")
  private Double temperature;

  public String getApiKey() {
    return apiKey;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getModel() {
    return model;
  }

  public Double getTemperature() {
    return temperature;
  }
}
