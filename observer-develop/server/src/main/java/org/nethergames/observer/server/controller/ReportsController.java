package org.nethergames.observer.server.controller;

import io.javalin.http.Context;
import io.javalin.openapi.*;
import org.nethergames.observer.data.reports.PlayerReportEntry;
import org.nethergames.observer.data.reports.PlayerReportEntry.PlayerReportList;
import org.nethergames.observer.data.reports.ReportResolution;
import org.nethergames.observer.data.reports.request.PlayerReportData;
import org.nethergames.observer.data.reports.request.PlayerReportRequest;
import org.nethergames.observer.server.Observer;

import java.util.Optional;

import static org.nethergames.observer.data.reports.PlayerReportEntry.*;

public class ReportsController {
    // Abstract: In-game, Make staff members be able to see a list of players who have reported
    // someone using "/report", for example, /stp would show an extra option, Below "Search Player"
    // and above "Settings" Which would show all in-game "/reports" players have sent.
    // Once having clicked "Player reports", It would show a list of reports sent by players,
    // The gamemode they were playing, Replay ID, Offenders name,
    // The player who reported the offender's name. Time + date.

    // Reporting player endpoint, basic endpoint to have when reporting player, the player will be stored in
    // a temporary cache where the player will be flushed from cache every 2 days.

    // When reporting a player, a mapping to another xuid will be added to the list.
    // This way, we can collect how much reports do the player received.

    // PUT: /report: Add a report to the xuid record.
    // Body content is PlayerReportData.

    @OpenApi(
            summary = "Submit a report for a player",
            path = "/report",
            methods = HttpMethod.PUT,
            tags = "Reports",
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = {
                            @OpenApiContent(from = PlayerReportData.class, mimeType = "application/json")
                    }
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "The report is recorded in the database successfully."),
                    @OpenApiResponse(status = "304", description = "The player was already reported by the reporter.")
            }
    )
    public static void reportPlayer(Context context) {
        var reportData = context.bodyAsClass(PlayerReportData.class);

        if (Observer.getObserver().getReportManager().addReport(reportData)) {
            context.status(200);
        } else {
            context.status(304);
        }
    }

    // POST /report: Get reports for the list of xuids.
    // Body is a list of string.

    @OpenApi(
            summary = "Retrieve all reports recorded by this player.",
            path = "/report",
            methods = HttpMethod.POST,
            tags = "Reports",
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = {
                            @OpenApiContent(from = PlayerReportRequest.class, mimeType = "application/json")
                    }
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PlayerReportMap.class), description = "The reports that was recorded by the list of players."),
            }
    )
    public static void getReportsBulk(Context context) {
        var reportData = context.bodyAsClass(PlayerReportRequest.class);

        context.json(Observer.getObserver().getReportManager().getReportsBulk(reportData));
    }

    // GET /report/{xuid}: Get all the reports for the given xuid
    // Return an object of ReportEntry

    @OpenApi(
            summary = "Retrieve all reports recorded by this player.",
            path = "/report/{xuid}",
            methods = HttpMethod.GET,
            tags = "Reports",
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true, description = "The player xuid", example = "2535418039503959")
            },
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = {
                            @OpenApiContent(from = PlayerReportData.class, mimeType = "application/json")
                    }
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PlayerReportEntry.class), description = "The reports that was recorded to this player."),
                    @OpenApiResponse(status = "404", description = "There were no reports found by this player.")
            }
    )
    public static void getReports(Context context) {
        var playerXuid = context.pathParam("xuid");
        var reports = Observer.getObserver().getReportManager().getReportsFor(playerXuid);

        if (reports == null) {
            context.status(404);
        } else {
            context.json(reports);
        }
    }

    // DELETE /report/{xuid}: Delete all report records for the xuid

    @OpenApi(
            summary = "Delete a report from the player records.",
            path = "/report/{xuid}",
            methods = HttpMethod.DELETE,
            tags = "Reports",
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true, description = "The player xuid", example = "2535418039503959")
            },
            queryParams = {
                    @OpenApiParam(name="resolution", description = "PUNISHED OR INSUFFICIENT")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "The report was successfully deleted."),
                    @OpenApiResponse(status = "404", description = "There were no reports found by this player.")
            }
    )
    public static void deleteReports(Context context) {
        var playerXuid = context.pathParam("xuid");

        var resolution = context.queryParam("resolution");

            if (!Observer.getObserver().getReportManager().deleteReports(playerXuid, resolution != null ? Optional.of(ReportResolution.valueOf(resolution)) : Optional.empty())) {
            context.status(404);
        }else{
            context.status(200);
        }
    }

    // GET /report/all-time: Return all time reports based on the recommended sets.
    // Parameter:
    // - page_limit: The page limit.

    @OpenApi(
            summary = "Mark report of player as claimed by trainee.",
            path = "/report/{xuid}/markTraineeClaimed",
            methods = HttpMethod.PATCH,
            tags = "Reports",
            pathParams = {
                    @OpenApiParam(name="xuid", required = true)
            },
            queryParams = {
                    @OpenApiParam(name = "xuid", type = Integer.class, description = "The page limit for the reports to show.", example = "15")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "The report was successfully marked."),
                    @OpenApiResponse(status = "404", description = "There were no reports found by this player.")
            }
    )
    public static void markTraineeClaimed(Context context) {
        var player = context.pathParam("xuid");
        var trainee = context.queryParam("traineexuid");

        if (Observer.getObserver().getReportManager().traineeClaimReport(player, trainee))
            context.status(200);
        else
            context.status(404);

    }

    @OpenApi(
            summary = "Get all reports sorted by the best possible records.",
            path = "/report/all-time",
            methods = HttpMethod.GET,
            tags = "Reports",
            queryParams = {
                    @OpenApiParam(name = "pageLimit", type = Integer.class, description = "The page limit for the reports to show.", example = "15")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PlayerReportList.class), description = "All time reports for the given list."),
            }
    )
    public static void getBestReports(Context context) {
        var pageLimit = context.queryParamAsClass("pageLimit", Integer.class).getOrDefault(50);

        context.json(Observer.getObserver().getReportManager().getReportsRecommended(pageLimit));
    }
}
