package org.nethergames.gsms.server;

import io.sentry.Sentry;
import lombok.extern.log4j.Log4j2;

@Log4j2(topic = "Bootstrap")
public class Bootstrap {
    public static void main(String[] args) {
        final var sentryDSN = System.getenv("SENTRY_DSN");
        if (sentryDSN != null) {
            Sentry.init(options -> {
                options.setSampleRate(0.3);
                options.setTracesSampleRate(0.3);
                options.setDsn(System.getenv("SENTRY_DSN"));
                options.setEnvironment(System.getenv("DOCS_ENABLED") != null ? "Testing" : "Production");
                options.setServerName("GSMS on " + System.getenv("HOSTNAME"));
            });
        } else {
            log.warn("Sentry is disabled");
        }

        try {
            (new GSMS()).start(startHook -> startHook.getWebServer().start(5000));
        } catch (Exception throwing) {
            log.throwing(throwing);

            Sentry.captureException(throwing);
        } finally {
            Sentry.flush(5000);
        }
    }
}
