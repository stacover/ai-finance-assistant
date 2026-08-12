package com.jh.aimodelgateway.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author jinhang
 * @since 2026/8/12 21:30
 */
@Data
@TableName("ai_conversation")
public class AIConversation {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("conversation_id")
  private String conversationId;

  @TableField("user_id")
  private String userId;

  @TableField("title")
  private String title;

  @TableField("status")
  private Integer status;
}
