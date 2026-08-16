package org.nethergames.gsms.server.web;

import io.javalin.http.Context;
import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.protocol.Request;
import lombok.extern.log4j.Log4j2;
import net.jodah.expiringmap.ExpiringMap;
import org.nethergames.gsms.data.general.ErrorResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Log4j2(topic = "Watcher")
public class JavalinUtil {
    public static void handleException(Throwable t, Context ctx) {
        String errorId = UUID.randomUUID().toString();

        Sentry.captureException(t);

        ctx.status(500);
        ctx.json(new ErrorResult<>("internal_error", t.getMessage(), errorId, null));
    }
}
