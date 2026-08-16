package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;
import org.nethergames.observer.data.punishment.Punishment;

public class AlreadyPunishedException extends RequestException {
    private final Punishment punishment;

    public AlreadyPunishedException(Punishment punishment) {
        super("already_banned", "That player already has a punishment", HttpStatus.CONFLICT, SpanStatus.ALREADY_EXISTS);
        this.punishment = punishment;
    }

    @Override
    public Object getInfo() {
        return punishment;
    }
}
