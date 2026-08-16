package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class PunishmentNotFoundException extends RequestException {
    private final String punishmentId;

    public PunishmentNotFoundException(String punishmentId) {
        super("punishment_not_found", "There was no punishment found with that id", HttpStatus.NOT_FOUND, SpanStatus.NOT_FOUND);
        this.punishmentId = punishmentId;
    }

    @Override
    public Object getInfo() {
        return this.punishmentId;
    }
}
