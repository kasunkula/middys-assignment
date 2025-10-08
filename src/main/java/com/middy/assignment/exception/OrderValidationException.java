package com.middy.assignment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OrderValidationException extends RuntimeException {
    private final HttpStatus httpStatus;

    public OrderValidationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public static class FutureOrderException extends OrderValidationException {
        private static final String ERROR_MESSAGE = "Order timestamp is in the future. Current time %s order timestamp %s";

        public FutureOrderException(long now, long timestamp) {
            super(String.format(ERROR_MESSAGE,
                            java.time.Instant.ofEpochMilli(now).toString(),
                            java.time.Instant.ofEpochMilli(timestamp).toString()
                    ), HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    public static class OldOrderException extends OrderValidationException {

        private static final String ERROR_MESSAGE = "Order timestamp is older than 60 seconds. Current time %s order timestamp %s";

        public OldOrderException(long now, String timestamp) {
            super(
                    String.format(ERROR_MESSAGE, java.time.Instant.ofEpochMilli(now).toString(), timestamp),
                    HttpStatus.NO_CONTENT
            );
        }

        public OldOrderException(long now, long timestamp) {
            this(now, java.time.Instant.ofEpochMilli(timestamp).toString());
        }
    }
}

