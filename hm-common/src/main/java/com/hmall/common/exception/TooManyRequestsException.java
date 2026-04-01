package com.hmall.common.exception;

/**
 * HTTP 429：请求过于频繁（令牌桶 / 限流）。
 */
public class TooManyRequestsException extends CommonException {

    public TooManyRequestsException(String message) {
        super(message, 429);
    }

    public TooManyRequestsException(String message, Throwable cause) {
        super(message, cause, 429);
    }
}
