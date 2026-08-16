package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class ArgumentMissingException extends RequestException {

    public ArgumentMissingException(String message, String reason) {
        super(message, reason, HttpStatus.BAD_REQUEST, SpanStatus.ABORTED);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
