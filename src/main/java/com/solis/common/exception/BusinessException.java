package com.solis.common.exception;

import lombok.Getter;

/**
 * 业务异常类，表示系统中发生的业务相关错误
 */
@Getter
public class BusinessException extends RuntimeException{
    /**
     * 业务错误码，用于前端/调用方做稳定的错误分支处理。
     */
    private final ErrorCode errorCode;

    /**
     * 使用错误码的默认文案构造异常
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
    /**
     * 使用自定义文案构造异常
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

}
