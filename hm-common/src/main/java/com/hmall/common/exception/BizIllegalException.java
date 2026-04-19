package com.hmall.common.exception;

public class BizIllegalException extends CommonException{

    public BizIllegalException(String message) {
        super(message, 400);
    }

    public BizIllegalException(String message, int code) {
        super(message, code);
    }

    public BizIllegalException(String message, int code, Throwable cause) {
        super(message, cause, code);
    }

    public BizIllegalException(String message, Throwable cause) {
        super(message, cause, 400);
    }

    public BizIllegalException(Throwable cause) {
        super(cause, 400);
    }
}
