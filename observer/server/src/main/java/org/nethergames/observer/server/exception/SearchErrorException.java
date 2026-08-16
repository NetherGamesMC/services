package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class SearchErrorException  extends RequestException {

    public SearchErrorException() {
        super("search_error", "Search condition must at least has more than 1 entries", HttpStatus.BAD_REQUEST, SpanStatus.ABORTED);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
