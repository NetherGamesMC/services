package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class ReasonConflictException extends RequestException {

    private final String category;
    private final String name;

    public ReasonConflictException(String category, String name) {
        super("reason_conflicting", "Punishment reason is conflicting with another punishment reason.", HttpStatus.CONFLICT, SpanStatus.ABORTED);

        this.category = category;
        this.name = name;
    }

    @Override
    public Object getInfo() {
        return "Category: " + category + ", Name: " + name;
    }
}
