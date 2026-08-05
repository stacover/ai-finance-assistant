package com.jh.aimodelgateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * @author jinhang
 * @since 2026/8/5 21:03
 **/
public record TechnicalAnswer(@NotBlank(message = "结论不能为空")
                              String conclusion,
                              @NotBlank(message = "解释不能为空")
                              String explanation,
                              @NotEmpty(message = "关键点不能为空")
                              List<@NotBlank String> keyPoints) {
}
