package org.nethergames.observer.server;

import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import org.nethergames.observer.server.controller.*;

import static io.javalin.apibuilder.ApiBuilder.*;
import static org.nethergames.observer.server.util.JavalinUtil.proxyRequest;

public class PathRegistry {
    public static void init() {
        path("/player", () -> {
            post("/ip", ctx -> proxyRequest(ctx, PlayerController::getXuidAddress));

            path("/{xuid}", () -> {
                get("/trace", ctx -> proxyRequest(ctx, PlayerController::tracePlayer));
                delete("/unlink", ctx -> proxyRequest(ctx, PlayerController::unlinkPlayer));

                get(ctx -> proxyRequest(ctx, PlayerController::getPointMapping));
                get("/explain", ctx -> proxyRequest(ctx, PlayerController::getPointExplanation));
                post(ctx -> proxyRequest(ctx, PlayerController::postPointMapping));
            });
        });

        put("/kick", ctx -> proxyRequest(ctx, KickController::kickPlayer));

        path("/punishment", () -> {
            path("/reasons", () -> {
                get(ctx -> proxyRequest(ctx, ReasonController::getReasons));
                put(ctx -> proxyRequest(ctx, ReasonController::insertReason));

                path("/{name}", () -> {
                    patch(ctx -> proxyRequest(ctx, ReasonController::patchReason));
                    delete(ctx -> proxyRequest(ctx, ReasonController::deleteReason));
                });
            });

            path("/{id}", () -> {
                get(ctx -> proxyRequest(ctx, PunishmentController::getPunishmentById));
                patch(ctx -> proxyRequest(ctx, PunishmentController::setPunishmentById));
                delete(ctx -> proxyRequest(ctx, PunishmentController::deletePunishmentById));

                path("/evidence", () -> {
                    put(ctx -> proxyRequest(ctx, EvidenceController::uploadEvidence));
                    get(ctx -> proxyRequest(ctx, EvidenceController::getEvidence));
                    patch(ctx -> proxyRequest(ctx, EvidenceController::updateEvidenceNote));
                    delete(ctx -> proxyRequest(ctx, EvidenceController::deleteEvidence));
                });
            });

            path("/player", () -> {
                post(ctx -> proxyRequest(ctx, PunishmentController::getPunishments));
                put(ctx -> proxyRequest(ctx, PunishmentController::createPunishment));

                get("/{xuid}", ctx -> proxyRequest(ctx, PunishmentController::getPunishmentsFor));
            });

            post("/search", ctx -> proxyRequest(ctx, PunishmentController::searchForPunishment));
        });

        path("/evidence/{report_id}", () -> {
            put(ctx -> proxyRequest(ctx, EvidenceController::uploadTemporaryEvidence));
            get(ctx -> proxyRequest(ctx, EvidenceController::redirectTemporaryEvidence));
            post(ctx -> proxyRequest(ctx, EvidenceController::patchTemporaryEvidence));
        });

        path("/report", () -> {
            put(ctx -> proxyRequest(ctx, ReportsController::reportPlayer));
            post(ctx -> proxyRequest(ctx, ReportsController::getReportsBulk));
            get("/all-time", ctx -> proxyRequest(ctx, ReportsController::getBestReports));

            path("/{xuid}", () -> {
                get(ctx -> proxyRequest(ctx, ReportsController::getReports));
                delete(ctx -> proxyRequest(ctx, ReportsController::deleteReports));
                patch("/markTraineeClaimed", ctx -> proxyRequest(ctx, ReportsController::markTraineeClaimed));
            });
        });

        get("/health", InstanceController::health);
        get("/metrics", ctx -> ctx.contentType(ContentType.TEXT_PLAIN).result(Observer.getObserver().getMetricsRegistry().scrape()));
    }
}
