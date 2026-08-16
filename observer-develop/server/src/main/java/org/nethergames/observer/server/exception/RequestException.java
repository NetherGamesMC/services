package org.nethergames.observer.server.exception;

import io.javalin.http.HttpStatus;
import io.sentry.SpanStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class RequestException extends RuntimeException {
    private String identifier;
    private String message;
    private HttpStatus code;
    private SpanStatus sentryStatus;

    public abstract Object getInfo();
}
