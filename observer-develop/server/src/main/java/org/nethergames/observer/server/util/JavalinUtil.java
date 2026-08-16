package org.nethergames.observer.server.util;

import io.javalin.http.Context;
import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.protocol.Request;
import lombok.extern.log4j.Log4j2;
import net.jodah.expiringmap.ExpiringMap;
import org.nethergames.observer.data.general.ErrorResult;
import org.nethergames.observer.server.exception.RequestException;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Log4j2(topic = "Watcher")
public class JavalinUtil {

    private static final ExpiringMap<UUID, ITransaction> transactions;

    static {
        transactions = ExpiringMap.builder().expiration(15, TimeUnit.MINUTES).build();
        transactions.addExpirationListener((key, transaction) -> transaction.finish(SpanStatus.ABORTED));
    }

    public static void handleException(Throwable t, Context ctx) {
        if (!(t instanceof RequestException)) {
            log.throwing(t);
        }

        ISpan span;
        String errorId;

        Object session = ctx.consumeSessionAttribute("transaction");
        if (session instanceof UUID) {
            span = transactions.remove((UUID) session);
            errorId = session.toString();

            span.setThrowable(t);
            span.setTag("errorId", errorId);
        } else {
            span = null;
            errorId = UUID.randomUUID().toString();
        }

        if (t instanceof RequestException requestException) {
            ctx.status(requestException.getCode());
            if (span != null) {
                span.finish(requestException.getSentryStatus());
            }
            ctx.json(new ErrorResult<>(requestException.getIdentifier(), requestException.getMessage(), errorId, requestException.getInfo()));
        } else {
            Sentry.captureException(t);

            ctx.status(500);
            if (span != null) {
                span.finish(SpanStatus.INTERNAL_ERROR);
            }

            ctx.json(new ErrorResult<>("internal_error", t.getMessage(), errorId, null));
        }
    }

    public static void proxyRequest(Context ctx, Consumer<Context> proxyTarget) {
        Sentry.configureScope(scope -> {
            Request request = new Request();
            request.setUrl(ctx.url());
            request.setHeaders(ctx.headerMap());

            var contentType = ctx.contentType();
            if (contentType != null && contentType.equalsIgnoreCase("application/json")) {
                request.setData(ctx.body());
            }

            request.setQueryString(ctx.queryString());
            request.setMethod(ctx.method().name());
            scope.setRequest(request);
        });

        UUID random;
        ctx.sessionAttribute("transaction", random = UUID.randomUUID());
        transactions.put(random, Sentry.startTransaction(ctx.matchedPath(), ctx.method().name()));

        proxyTarget.accept(ctx);
    }

    public static void afterHandler(Context ctx) {
        Object session = ctx.consumeSessionAttribute("transaction");
        if (!(session instanceof UUID)) {
            return;
        }

        ISpan span = transactions.remove((UUID) session);

        if (span != null) {
            if (span.getStatus() == null) {
                span.setStatus(SpanStatus.OK);
            }

            span.finish();
        }
    }
}
