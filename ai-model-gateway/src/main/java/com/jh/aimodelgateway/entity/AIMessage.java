package com.jh.aimodelgateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author jinhang
 * @since 2026/8/12 21:32
 */
@Data
@TableName("ai_message")
public class AIMessage {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("conversation_id")
  private String conversationId;

  @TableField("role")
  private String role;

  @TableField("content")
  private String content;

  @TableField("model")
  private String model;

  @TableField("input_tokens")
  private Integer inputTokens;

  @TableField("output_tokens")
  private Integer outputTokens;

  @TableField("total_tokens")
  private Integer totalTokens;

  @TableField("duration_ms")
  private Long durationMs;

  @TableField("finish_reason")
  private String finishReason;

  @TableField("create_time")
  private LocalDateTime createTime;
}
