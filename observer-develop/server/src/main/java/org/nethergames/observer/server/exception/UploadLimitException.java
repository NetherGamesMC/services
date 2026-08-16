package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class UploadLimitException extends RequestException {
    public UploadLimitException() {
        super("upload_limit_reached", "Evidence attached to this punishment has reached its limit.", HttpStatus.BAD_REQUEST, SpanStatus.ABORTED);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
