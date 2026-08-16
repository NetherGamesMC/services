package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class ParseErrorException extends RequestException {
    private final String body;

    public ParseErrorException(String message) {
        this(message, "");
    }

    public ParseErrorException(String message, String body) {
        super("parse_error_exception", message, HttpStatus.INTERNAL_SERVER_ERROR, SpanStatus.INTERNAL_ERROR);
        this.body = body;
    }

    @Override
    public Object getInfo() {
        return this.body;
    }
}
