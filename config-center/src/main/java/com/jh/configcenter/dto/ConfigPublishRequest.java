package com.jh.configcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 配置发布请求体。
 *
 * @author jinhang
 * @since 2026/8/12
 */
public class ConfigPublishRequest {

  /** 配置 DataId，如 ai-model-gateway.yaml */
  @NotBlank(message = "dataId 不能为空")
  private String dataId;

  /** 配置分组，默认 DEFAULT_GROUP */
  @Pattern(regexp = "^$|^[A-Za-z0-9_\\-]+$", message = "group 只能包含字母、数字、下划线和连字符")
  private String group = "DEFAULT_GROUP";

  /** 配置内容 */
  private String content;

  public String getDataId() {
    return dataId;
  }

  public void setDataId(String dataId) {
    this.dataId = dataId;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
