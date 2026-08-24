package com.jh.aimodelgateway.exception;

import lombok.Getter;

/**
 * @author jinhang
 * @since 2026/8/24 22:06
 */

@Getter
public class AiModelException extends RuntimeException {

    private final AiErrorCode errorCode;

    public AiModelException(
            AiErrorCode errorCode,
            Throwable cause
    ) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public AiModelException(AiErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
