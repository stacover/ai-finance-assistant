package com.jh.aimodelgateway.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author jinhang
 * @since 2026/8/3 22:13
 **/
public record ChatRequest( @NotNull(message = "问题内容不能为空")
                           @Size(max = 2000, message = "问题内容不能超过2000个字符")String message) {
}
