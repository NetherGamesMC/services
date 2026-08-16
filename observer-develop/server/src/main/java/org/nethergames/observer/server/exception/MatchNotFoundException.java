package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class MatchNotFoundException extends RequestException {
    private final String matchId;

    public MatchNotFoundException(String matchId) {
        super("match_not_found", "No such match found", HttpStatus.NOT_FOUND, SpanStatus.NOT_FOUND);

        this.matchId = matchId;
    }

    @Override
    public Object getInfo() {
        return this.matchId;
    }
}
