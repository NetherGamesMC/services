package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class UploadIllegalException extends RequestException {
    public UploadIllegalException() {
        super("illegal_upload_type", "AWS managed object can only be used internally.", HttpStatus.UNAUTHORIZED, SpanStatus.INVALID_ARGUMENT);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
