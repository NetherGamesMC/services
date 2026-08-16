package org.nethergames.observer.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.nethergames.observer.data.general.ErrorResult;
import org.nethergames.observer.data.punishment.PunishmentReason;
import org.nethergames.observer.data.punishment.PunishmentReasonGrouped;
import org.nethergames.observer.server.exception.MatchNotFoundException;
import org.nethergames.observer.server.exception.ParseErrorException;
import org.nethergames.observer.server.exception.ReasonConflictException;
import org.nethergames.observer.server.manager.MongoManager;

import java.util.Map;

public class ReasonController {

    private static final ObjectMapper mapper = new ObjectMapper();

    // GET
    // - /punishment/reasons: Get all the reasons available.
    // Query parameters:
    // - grouped (Boolean): Enable grouping of the reason by category.

    @OpenApi(
            summary = "Gets all available punishment reasons, either unordered or grouped by category",
            methods = HttpMethod.GET,
            path = "/punishment/reasons",
            tags = "Punishment Reasons",
            queryParams = {
                    @OpenApiParam(name = "grouped", type = Boolean.class, example = "false"),
                    @OpenApiParam(name = "filter", type = Boolean.class, example = "false")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "The list or group of reasons available.", content = {
                            @OpenApiContent(from = PunishmentReason[].class),
                            @OpenApiContent(from = PunishmentReasonGrouped.class)
                    }),
            }
    )
    public static void getReasons(Context context) {
        boolean grouped = context.queryParamAsClass("grouped", Boolean.class).getOrDefault(false);
        boolean filterPublic = context.queryParamAsClass("filter", Boolean.class).getOrDefault(false);

        if (grouped) {
            context.json(MongoManager.getGroupedPunishmentReasons(filterPublic));
        } else {
            context.json(MongoManager.getPunishmentReasons(filterPublic));
        }
    }

    // PUT, PATCH, DELETE
    // When doing PATCH, one would have to determine if the punishment itself do exists, therefore to do that
    // we will have to introduce a parameter in the reasons with:
    // - /punishment/reasons/{category}/{name}: Update any punishment with the category and name.
    // In PATCH, DELETE: Error 404 will be thrown if the reason does not exist.
    // In PUT: Error 409 - Conflict (If one reason is already present

    @OpenApi(
            summary = "Insert a new punishment reason into database.",
            description = "The method here are used to only insert a new punishment, updating a punishment require the use of another patch endpoint as this.",
            methods = HttpMethod.PUT,
            path = "/punishment/reasons",
            tags = "Punishment Reasons",
            requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = PunishmentReason.class)),
            responses = {
                    @OpenApiResponse(status = "200", description = "The operation was successful"),
                    @OpenApiResponse(status = "409", description = "Another punishment with the same name and category already exists in database", content = @OpenApiContent(from = ErrorResult.class))
            }
    )
    public static void insertReason(Context context) {
        var reason = context.bodyAsClass(PunishmentReason.class);
        var reasonExists = MongoManager.getPunishmentReason(reason.getName());
        if (reasonExists != null) {
            throw new ReasonConflictException(reasonExists.getCategory(), reasonExists.getName());
        } else {
            MongoManager.insertPunishment(reason);

            context.json(reason);
        }
    }

    @OpenApi(
            summary = "Update an existing punishment reason in the database with the punishments assigned to it.",
            description = "This method is used to update the reason for punishment within the punishment_reasons database, as well as to update all punishments that have the same category and name",
            methods = HttpMethod.PATCH,
            path = "/punishment/reasons/{name}",
            tags = "Punishment Reasons",
            pathParams = {
                    @OpenApiParam(name = "name", required = true, description = "The name for the punishment", example = "Being Funny"),
            },
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = PunishmentReason.class)),
            responses = {
                    @OpenApiResponse(status = "200", description = "The operation was successful"),
                    @OpenApiResponse(status = "404", description = "The punishment reason could not found", content = @OpenApiContent(from = MatchNotFoundException.class))
            }
    )
    public static void patchReason(Context context) {
        var name = context.pathParam("name");

        var reason = MongoManager.getPunishmentReason(name);
        if (reason == null) {
            throw new MatchNotFoundException("Name: " + name);
        } else {
            Map<String, Object> modelAsMap;
            Map<String, Object> patchedModel;

            try {
                modelAsMap = mapper.convertValue(reason, new TypeReference<>() {});
                patchedModel = mapper.readValue(context.body(), new TypeReference<>() {});
            } catch (JsonProcessingException error) {
                throw new ParseErrorException(error.getMessage(), context.body());
            }

            patchedModel.entrySet().stream().filter(e -> modelAsMap.containsKey(e.getKey())).forEach(e -> modelAsMap.put(e.getKey(), e.getValue()));

            reason = mapper.convertValue(modelAsMap, PunishmentReason.class);

            if (MongoManager.updatePunishmentReason(name, reason)) {
                context.json(reason);
            } else {
                context.status(HttpStatus.NO_CONTENT);
            }
        }
    }

    @OpenApi(
            summary = "Delete a punishment reason from the database.",
            description = "The method for this operation is to remove the reason from the database. Note that this does not remove the punishments themselves from the database, which must be done explicitly.",
            methods = HttpMethod.DELETE,
            path = "/punishment/reasons/{name}",
            tags = "Punishment Reasons",
            pathParams = {
                    @OpenApiParam(name = "name", required = true, description = "The name for the punishment.", example = "Being Funny"),
            },
            queryParams = @OpenApiParam(name = "deletePunishments", type = Boolean.class, example = "false"),
            responses = {
                    @OpenApiResponse(status = "200", description = "The operation was successful"),
                    @OpenApiResponse(status = "404", description = "The punishment reason could not found", content = @OpenApiContent(from = MatchNotFoundException.class))
            }
    )
    public static void deleteReason(Context context) {
        var punishmentName = context.pathParam("name");
        var deletePunishments = context.queryParamAsClass("deletePunishments", Boolean.class).getOrDefault(false);

        if (!MongoManager.deletePunishmentReason(punishmentName)) {
            throw new MatchNotFoundException("Name: " + punishmentName);
        }

        if (deletePunishments) {
            MongoManager.deletePunishmentByReason(punishmentName);
        }

        context.status(200);
    }
}
