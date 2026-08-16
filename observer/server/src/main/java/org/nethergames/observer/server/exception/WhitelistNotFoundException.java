package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class WhitelistNotFoundException extends RequestException {

    public WhitelistNotFoundException() {
        super("whitelist_entry_not_found", "", HttpStatus.NOT_FOUND, SpanStatus.NOT_FOUND);
    }

    @Override
    public Object getInfo() {
        return null;
    }
}
