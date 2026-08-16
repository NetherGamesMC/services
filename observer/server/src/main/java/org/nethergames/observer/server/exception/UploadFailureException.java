package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class UploadFailureException extends RequestException {
    private final String trace;

    public UploadFailureException(String message, String stackTrace) {
        super("upload_failure", message, HttpStatus.INTERNAL_SERVER_ERROR, SpanStatus.ABORTED);

        this.trace = stackTrace;
    }

    @Override
    public Object getInfo() {
        return trace;
    }
}
