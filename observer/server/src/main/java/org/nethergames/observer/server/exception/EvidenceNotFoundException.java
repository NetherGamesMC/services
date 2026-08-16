package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;

public class EvidenceNotFoundException extends RequestException {
    private final String evidenceId;

    public EvidenceNotFoundException(String evidenceId) {
        super("evidence_not_found", "There was no evidence found with that id", HttpStatus.NOT_FOUND, SpanStatus.NOT_FOUND);
        this.evidenceId = evidenceId;
    }

    @Override
    public Object getInfo() {
        return this.evidenceId;
    }
}
