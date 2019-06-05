package com.github.hept59434091.reactor.exc;

/**
 * RegisterException
 *
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-05-05
 */
public class RegisterException extends RuntimeException {
    public RegisterException(String message, Throwable cause) {
        super(message, cause);
    }
}
