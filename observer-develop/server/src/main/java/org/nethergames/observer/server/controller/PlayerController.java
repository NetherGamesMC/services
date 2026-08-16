package org.nethergames.observer.server.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.nethergames.observer.data.metadata.PlayerAddressRequestList;
import org.nethergames.observer.data.metadata.PlayerAddressResponseMap;
import org.nethergames.observer.data.punishment.PointMapping;
import org.nethergames.observer.data.punishment.request.punishment.PlayerStatus;
import org.nethergames.observer.data.punishment.type.PunishmentType;
import org.nethergames.observer.data.tracing.AltTracingPushData;
import org.nethergames.observer.data.tracing.request.TracingSearchEntry;
import org.nethergames.observer.data.tracing.type.TracingType;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.ArgumentMissingException;
import org.nethergames.observer.server.exception.ParseErrorException;
import org.nethergames.observer.server.manager.TracingManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.groupingBy;

public class PlayerController {

    private static final TracingManager tracingManager = Observer.getObserver().getTracingManager();

    @OpenApi(
            summary = "Return player ban and mute status.",
            path = "/player/{xuid}",
            methods = HttpMethod.GET,
            description = "Get the status of the player. By default, this includes the active ban (or null), active mute(or null), a list of staff comments, and if toggled by the queryParam, the list point mapping of the player (or null if not)",
            tags = "Player Controller",
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true)
            },
            queryParams = {
                    @OpenApiParam(name = "includePointMaps", type = Boolean.class),
                    @OpenApiParam(name = "depth", type = Integer.class, description = "The depth for the tracing algorithm to search for, depth of `-1` will disable tracing completely. (Default -1)", example = "-1")
            },
            responses = {
                    @OpenApiResponse(status = "202", content = @OpenApiContent(from = PlayerStatus.class)),
                    @OpenApiResponse(status = "404", description = "There was no player found with the given xuid")
            }
    )
    public static void getPointMapping(Context context) {
        var xuid = context.pathParam("xuid");
        var includePointMaps = context.queryParamAsClass("includePointMaps", Boolean.class).getOrDefault(false);
        var depth = context.queryParamAsClass("depth", Integer.class).getOrDefault(-1);

        context.json(Observer.getObserver().getPunishmentManager().getPlayerStatus(xuid, depth, includePointMaps));
    }

    @OpenApi(
            path = "/player/{xuid}",
            summary = "Upsert tracing information and return player ban and mute.",
            description = "This method will attempt to update the dataset, then return the longest active punishment (ban and mute) for a player. Parameter `depth` will trace the punishments linked by other accounts matched by dataset available for the player.",
            methods = HttpMethod.POST,
            tags = "Player Controller",
            requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = AltTracingPushData.class)),
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true)
            },
            queryParams = {
                    @OpenApiParam(name = "includePointMaps", type = Boolean.class),
                    @OpenApiParam(name = "depth", type = Integer.class, description = "The depth for the tracing algorithm to search for, depth of -1 will disable tracing completely. (Default -1)", example = "-1")
            },
            responses = {
                    @OpenApiResponse(status = "202", content = @OpenApiContent(from = PlayerStatus.class)),
                    @OpenApiResponse(status = "404", description = "There was no player found with the given xuid")
            }
    )
    public static void postPointMapping(Context context) {
        var xuid = context.pathParam("xuid");
        var includePointMaps = context.queryParamAsClass("includePointMaps", Boolean.class).getOrDefault(false);
        var depth = context.queryParamAsClass("depth", Integer.class).getOrDefault(-1);

        // Update existing dataset with what the player has.
        AltTracingPushData tracingPushData = context.bodyAsClass(AltTracingPushData.class);
        tracingManager.updateDataset(tracingPushData);

        context.json(Observer.getObserver().getPunishmentManager().getPlayerStatus(xuid, depth, includePointMaps));
    }

    @OpenApi(
            path = "/player/ip",
            summary = "Return all IP addresses for the given list of xuid string.",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = PlayerAddressRequestList.class)),
            tags = "Player Controller",
            responses = {
                    @OpenApiResponse(status = "200", description = "Will return a map of players to its respective addresses.", content = @OpenApiContent(from = PlayerAddressResponseMap.class))
            }
    )
    public static void getXuidAddress(Context context) {
        PlayerAddressRequestList requestList = context.bodyAsClass(PlayerAddressRequestList.class);

        context.json(Observer.getObserver().getTracingManager().getAddressDatasetFor(requestList));
    }

    public static void getPointExplanation(Context context) {
        var xuid = context.pathParam("xuid");

        var pointPairs = new HashMap<String, Map<String, String>>();
        var players = Set.of(xuid);
        var punishments = Observer.getObserver().getPunishmentManager().getPunishments(players, false, PunishmentType.values());

        players.forEach(o -> pointPairs.put(o, new HashMap<>()));

        punishments.forEach((player_xuid, map) -> {
            var categoryPoint = new HashMap<String, String>();
            var categoryMap = map.stream().collect(groupingBy(o -> o.getReason().getCategory()));

            categoryMap.forEach((category, punishmentList) -> categoryPoint.put(category, Observer.getObserver().getPunishmentManager().calculatePointMapping(punishmentList).getBreakdown()));

            pointPairs.put(xuid, categoryPoint);
        });

        context.json(pointPairs.get(xuid));
    }

    @OpenApi(
            path = "/player/{xuid}/trace",
            summary = "Trace subsequent XUIDs that have a one-to-one relationship with the given XUID.",
            description = "To trace a player and identify deeply linked accounts, we use self-signed IDs, device IDs, and IP addresses. This operation may take longer than a second, depending on the specified depth and the number of accounts being linked. A depth always starts at 0.",
            methods = HttpMethod.GET,
            tags = "Player Controller",
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true, type = Integer.class, example = "2535469883116462"),
            },
            queryParams = {
                    @OpenApiParam(name = "searchConditions", description = "The search condition separated by commas. (Example: IP,DEVICE_ID,SELF_SIGNED_ID)"),
                    @OpenApiParam(name = "depth", type = Integer.class, example = "0")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Will return a Map<Integer(Level), List<String>(Matched alt accounts)>")
            }
    )
    public static void tracePlayer(Context context) {
        var xuid = context.pathParam("xuid");
        var depth = context.queryParamAsClass("depth", Integer.class).getOrDefault(0);
        var searchConditions = context.queryParamAsClass("searchConditions", String.class).getOrDefault("").split(",");

        var data = EnumSet.noneOf(TracingType.class);
        for (var condition : searchConditions) {
            if (condition.isEmpty()) {
                continue;
            }

            try {
                data.add(TracingType.valueOf(condition));
            } catch (IllegalArgumentException error) {
                throw new ParseErrorException(error.getMessage(), context.body());
            }
        }

        if (data.isEmpty()) {
            data = EnumSet.allOf(TracingType.class);
        }

        context.json(Observer.getObserver().getTracingManager().recursiveSearch(new TracingSearchEntry(depth, Set.of(xuid), data)).get(xuid));
    }

    @OpenApi(
            path = "/player/{xuid}/unlink",
            summary = "Unlink a player xuid from another alt-account.",
            description = "The unlink process will remove all references which contains device ids, self-signed ids, and IPs from the given actor xuid data to the player xuid.",
            methods = HttpMethod.DELETE,
            tags = "Player Controller",
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true, type = Integer.class, example = "2535469883116462"),
            },
            queryParams = {
                    @OpenApiParam(name = "actorXuid", description = "The actor xuid that will be removed."),
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Will return a List<String>(Device ID, Self-Signed IDs, IPs) indicating number of dataset that was deleted for the player xuid.")
            }
    )
    public static void unlinkPlayer(Context context) {
        var xuid = context.pathParam("xuid");
        var actor = context.queryParamAsClass("actorXuid", String.class).getOrThrow(x -> new ArgumentMissingException("missing_actor_xuid", "Actor xuid is required to determine dataset that is to be deleted."));

        ArrayList<Integer> result = Observer.getObserver().getTracingManager().unlinkPlayerTrace(xuid, actor);

        if (result == null) {
            context.status(HttpStatus.NOT_FOUND);
        } else {
            context.json(result);
        }
    }
}
