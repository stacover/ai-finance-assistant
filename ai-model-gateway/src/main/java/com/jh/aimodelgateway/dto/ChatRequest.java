package com.jh.aimodelgateway.dto;

import jakarta.validation.constraints.NotBlank;

import javax.validation.constraints.Size;

/**
 * @author jinhang
 * @since 2026/8/3 22:13
 **/
public record ChatRequest( @NotBlank(message = "会话ID不能为空")
                           @Size(max = 64, message = "会话ID不能超过64个字符")
                           String conversationId,

                           @NotBlank(message = "问题内容不能为空")
                           @Size(max = 2000, message = "问题内容不能超过2000个字符")
                           String message) {
}
