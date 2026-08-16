package org.nethergames.observer.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jkeylockmanager.manager.exception.KeyLockManagerException;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import lombok.SneakyThrows;
import org.nethergames.observer.data.punishment.Punishment;
import org.nethergames.observer.data.punishment.request.*;
import org.nethergames.observer.data.punishment.request.punishment.BulkPunishment;
import org.nethergames.observer.data.punishment.request.punishment.BulkPunishmentGrouped;
import org.nethergames.observer.data.punishment.type.PunishmentType;
import org.nethergames.observer.data.tracing.request.TracingWhitelistEntry;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.ParseErrorException;
import org.nethergames.observer.server.exception.PunishmentNotFoundException;
import org.nethergames.observer.server.manager.MongoManager;
import org.nethergames.observer.server.manager.PunishmentManager;
import org.nethergames.observer.server.manager.TracingManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PunishmentController {
    private static final TracingManager tracingManager = Observer.getObserver().getTracingManager();
    private static final PunishmentManager punishmentManager = Observer.getObserver().getPunishmentManager();
    private static final ObjectMapper mapper = new ObjectMapper();

    // The /player endpoint is a Punishment Endpoint Category for given xuid parameter.
    // xuid in this parameter is ALWAYS the OFFENDER.

    // The new endpoint should be as follows:

    // POST:
    // - /punishment/create: Punish a player for a given Punishment
    // Body of the POST is a PunishmentCreationData (In JSON).
    // Throws 409 - timeout when the existing player is already trying to be punished
    @OpenApi(
            summary = "Create and publish a new punishment",
            path = "/punishment/player",
            methods = HttpMethod.PUT,
            tags = "Punishment",
            requestBody = @OpenApiRequestBody(
                    required = true,
                    content = @OpenApiContent(from = PunishmentCreationData.class)
            ),
            responses = {
                    @OpenApiResponse(status = "200", description = "Punishment is successfully created.", content = @OpenApiContent(from = Punishment.class)),
                    @OpenApiResponse(status = "409", description = "Request conflict, punishment for the player is already in progress - timed-out when trying to wait for punishment to complete.")
            }
    )
    public static void createPunishment(Context context) {
        PunishmentCreationData punishment = context.bodyAsClass(PunishmentCreationData.class);

        try {
            context.json(punishmentManager.createPunishment(punishment));
        } catch (KeyLockManagerException ignored) {
            context.status(HttpStatus.CONFLICT);
        }
    }

    // GET:
    // - /punishment/player: Bulk punishments for the list of xuids.
    // Body of the GET is a string array.
    // Returns an array of Punishments.
    // Parameter inside list of xuids:
    // - tracing_option (1 or 0): Return punishments that were traced to the xuid.
    // - depth (default 0): Depth of the punishment if tracing_option is enabled.
    // - grouped (default 0): Group all punishment returned by the punishment category.

    @OpenApi(
            summary = "Get a list of punishments from a list of xuid",
            path = "/punishment/player",
            methods = HttpMethod.POST,
            tags = "Punishment",
            queryParams = @OpenApiParam(name = "grouped", description = "Group all punishment returned by the punishment category.", type = Boolean.class, example = "false"),
            requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = PunishmentRequestData[].class)),
            responses = {
                    @OpenApiResponse(status = "200", description = "All punishments were found without grouping", content = {
                            @OpenApiContent(from = BulkPunishment[].class),
                            @OpenApiContent(from = BulkPunishmentGrouped[].class)
                    })
            }
    )
    public static void getPunishments(Context context) {
        var grouped = context.queryParamAsClass("grouped", Boolean.class).getOrDefault(false);
        var dataset = List.of(context.bodyAsClass(PunishmentRequestData[].class));

        if (grouped) {
            context.json(punishmentManager.getGroupedPunishments(dataset, true));
        } else {
            context.json(punishmentManager.getPunishments(dataset, true));
        }
    }

    // GET:
    // - /punishment/player/{xuid}: Return all punishments given to the xuid.
    // Query parameters:
    //  - depth (default 0): Depth of the punishment if tracing_option is enabled.
    //  - grouped (category or none): Punishments are grouped by category of the punishment.
    //  - activeOnly: Return an active only punishments.
    @OpenApi(
            summary = "Get a list of punishments presence for the given xuid",
            path = "/punishment/player/{xuid}",
            methods = HttpMethod.GET,
            tags = "Punishment",
            pathParams = {
                    @OpenApiParam(name = "xuid", required = true, description = "The player's xuid")
            },
            queryParams = {
                    @OpenApiParam(name = "grouped", description = "Return a map of punishments that are organized by categories, list will be returned if option not present", type = Boolean.class, example = "false"),
                    @OpenApiParam(name = "depth", description = "The depth for tracing punishment search. Default is -1", example = "-1"),
                    @OpenApiParam(name = "activeOnly", description = "Only return punishments that is active."),
                    @OpenApiParam(name = "withEvidence", description = "Returns punishment data with evidence field.")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "The list or group of punishments for a player.", content = {
                            @OpenApiContent(from = Punishment[].class),
                            @OpenApiContent(from = BulkPunishmentGrouped[].class)
                    })
            }
    )
    public static void getPunishmentsFor(Context context) {
        var grouped = context.queryParamAsClass("grouped", Boolean.class).getOrDefault(false);
        var withEvidence = context.queryParamAsClass("withEvidence", Boolean.class).getOrDefault(false);
        var dataset = List.of(new PunishmentRequestData(
                context.pathParam("xuid"),
                parseWithDefault(context.queryParam("depth"), -1),
                context.queryParam("activeOnly") != null,
                PunishmentType.values()
        ));

        if (grouped) {
            context.json(punishmentManager.getGroupedPunishments(dataset, withEvidence));
        } else {
            context.json(punishmentManager.getPunishments(dataset, withEvidence));
        }
    }

    // GET, PATCH, DELETE
    // - /punishment/{id}: Get information of a ban by id.
    // Query parameters:
    //  - traced (1 or 0): Get traced punishment or exclude the xuid from this trace.
    // Notes:
    // You cannot patch/edit a punishment based on the player's traced punishment, a traced punishment
    // can only be used with GET and DELETE - in this case, DELETE-ing a traced punishment simply put
    // the player into exclusion list. By default, the punishment's depth would ALWAYS be 0.

    @OpenApi(
            summary = "Get information of a punishment by an id",
            description = "This method is intended to get a punishment by an id and/or traced to the xuid back, you can use this endpoint to determine if the player is being punished by tracing method.",
            path = "/punishment/{id}",
            methods = HttpMethod.GET,
            tags = "Punishment",
            pathParams = @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf"),
            queryParams = @OpenApiParam(name = "traced_xuid", description = "Return an information of a punishment that is traced to given xuid.", example = "2535469883116462"),
            responses = {
                    @OpenApiResponse(status = "200", description = "The list or group of punishments for a player.", content = @OpenApiContent(from = Punishment.class)),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = PunishmentNotFoundException.class))
            }
    )
    public static void getPunishmentById(Context context) {
        var xuid = context.queryParam("traced_xuid");
        var id = context.pathParam("id");

        Punishment punishment;
        if (xuid == null) {
            punishment = MongoManager.getPunishment(id);
        } else {
            var punishments = punishmentManager.getPunishments(List.of(new PunishmentRequestData(xuid, 0, false, PunishmentType.values()))).stream().findFirst().orElse(null);
            if (punishments == null) {
                throw new PunishmentNotFoundException(id);
            }

            punishment = punishments.getPunishments().stream().filter(o -> o.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        }

        if (punishment == null) {
            throw new PunishmentNotFoundException(id);
        }

        punishment.setEvidences(Observer.getObserver().getEvidenceManager().getEvidences(punishment.getId()));

        context.json(punishment);
    }

    // PATCH
    @OpenApi(
            summary = "Update any punishment notes or evidence to the given id.",
            description = "This method is used to replace, update, or remove any punishment notes and/or evidences, patching a traced document is not supported.",
            path = "/punishment/{id}",
            methods = HttpMethod.PATCH,
            tags = "Punishment",
            pathParams = @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf"),
            responses = {
                    @OpenApiResponse(status = "200", description = "The list or group of punishments for a player.", content = @OpenApiContent(from = Punishment.class)),
                    @OpenApiResponse(status = "204", description = "The punishment was not changed, the existing punishment already has the same values."),
                    @OpenApiResponse(status = "404", description = "No punishment could be found with the given punishment id", content = @OpenApiContent(from = PunishmentNotFoundException.class)),
                    @OpenApiResponse(status = "500", description = "The body is erroneous, validate your body content then retry.", content = @OpenApiContent(from = ParseErrorException.class))
            }
    )
    public static void setPunishmentById(Context context) {
        var id = context.pathParam("id");

        var punishment = MongoManager.getPunishment(id);

        if (punishment == null) {
            throw new PunishmentNotFoundException(id);
        } else {
            Map<String, Object> modelAsMap;
            Map<String, Object> patchedModel;

            try {
                modelAsMap = mapper.convertValue(punishment, new TypeReference<>() {
                });
                patchedModel = mapper.readValue(context.body(), new TypeReference<>() {
                });
            } catch (JsonProcessingException error) {
                throw new ParseErrorException(error.getMessage(), context.body());
            }

            // Only allow modifying "note" and "evidence" fields.
            patchedModel.entrySet().stream().filter(e -> modelAsMap.containsKey(e.getKey()) && List.of("note", "evidence").contains(e.getKey())).forEach(e -> modelAsMap.put(e.getKey(), e.getValue()));

            punishment = mapper.convertValue(modelAsMap, Punishment.class);

            if (MongoManager.updatePunishment(punishment)) {
                context.json(punishment);
            } else {
                context.status(HttpStatus.NO_CONTENT);
            }
        }
    }

    @OpenApi(
            summary = "Delete any punishment by an id",
            description = "This method is intended to delete a punishment *and* whitelist a player from a punishment, providing tracedXuid field will always put the xuid into tracing whitelist.",
            path = "/punishment/{id}",
            methods = HttpMethod.DELETE,
            tags = "Punishment",
            pathParams = @OpenApiParam(name = "id", required = true, description = "The punishment id", example = "0109b06caf"),
            queryParams = {
                    @OpenApiParam(name = "tracedXuid", description = "An xuid for the player that is traced to this punishment.", example = "2535469883116462"),
                    @OpenApiParam(name = "issuer", description = "The xuid for a moderator that requests for deletion of the specified punishment.", example = "2535418039503959")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Punishment.class), description = "The deletion or whitelisting is successful."),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = PunishmentNotFoundException.class))
            }
    )
    public static void deletePunishmentById(Context context) {
        var xuid = context.queryParamAsClass("tracedXuid", String.class).allowNullable().get();
        var issuer = context.queryParamAsClass("issuer", String.class).getOrDefault("2535418039503959"); // this is NetherGamesMC xuid
        var id = context.pathParam("id");

        Punishment punishment;
        if (xuid == null) {
            punishment = MongoManager.getPunishment(id);
            if (punishment == null || !MongoManager.deletePunishment(id)) {
                throw new PunishmentNotFoundException(id);
            }

            MongoManager.deleteUsernameEntry(id);

            Observer.getObserver().getKafkaManager().punishmentsRemoved(new PunishmentRemovalAction(issuer, Collections.singletonList(punishment)));
        } else {
            var punishments = punishmentManager.getPunishments(List.of(new PunishmentRequestData(xuid, 0, false, PunishmentType.values()))).stream().findFirst().orElse(null);

            if (punishments == null) {
                throw new PunishmentNotFoundException(id);
            }

            punishment = punishments.getPunishments().stream().filter(o -> o.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (punishment == null) {
                throw new PunishmentNotFoundException(id);
            }

            Observer.getObserver().getKafkaManager().punishmentsWhitelisted(new PunishmentWhitelistAction(issuer, Collections.singletonList(punishment)));

            tracingManager.addWhitelistEntry(new TracingWhitelistEntry(xuid, punishment.getXuid()));
        }

        context.json(punishment);
    }

    // POST /punishment/search
    // Parameter:
    //  - xuid:         Should be the target that we want to search?
    //  - issuer_xuid:  Who was issuing the ban?
    //  - after:        After the given date.
    //  - before:       Before the given date. (If `after` parameter exists, check if after < before)
    //  - category:     Category of a ban we are looking for.
    //  - evidence_type (enum):
    //    - ALL:            Return all punishments
    //    - ONLY_SUBMITTED: Return all punishments that are submitted.
    //    - NOT_SUBMITTED:  Only returned punishments that has no evidence attached.

    @OpenApi(
            summary = "Search for a punishment",
            description = "Search for any punishment within the specified search configuration.",
            path = "/punishment/search",
            methods = HttpMethod.POST,
            tags = "Punishment",
            queryParams = {
                    @OpenApiParam(name = "offset", example = "0", description = "The page of the searched punishment", type = Long.class),
                    @OpenApiParam(name = "withEvidence", example = "true", description = "Resolves evidences for punishments", type = Boolean.class)
            },
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PunishmentSearchRequest.class), description = "The search conditions query."),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PunishmentSearchData.class), description = "The page of a set of punishments, limited up to 50 punishments per requests."),
                    @OpenApiResponse(status = "404", content = @OpenApiContent(from = PunishmentNotFoundException.class))
            }
    )
    @SneakyThrows
    public static void searchForPunishment(io.javalin.http.Context context) {
        var reason = context.bodyAsClass(PunishmentSearchRequest.class);
        var offset = context.queryParamAsClass("offset", Long.class).getOrDefault(0L);
        var withEvidence = context.queryParamAsClass("withEvidence", Boolean.class).getOrDefault(false);

        context.json(MongoManager.searchPunishment(reason, offset, withEvidence));
    }

    public static int parseWithDefault(String number, int defaultVal) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
