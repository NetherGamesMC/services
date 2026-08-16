package org.nethergames.gsms.server.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.data.Region;
import org.nethergames.gsms.data.model.*;
import org.nethergames.gsms.server.manager.ProxyManager;
import org.nethergames.gsms.server.matchmaking.Matchmaker;
import org.nethergames.gsms.server.server.ServerRegistry;
import org.nethergames.utils.server.ServerUniqueId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Log4j2(topic = "WebServer")
public class WebServer {
    private final ServerRegistry registry;
    private final Matchmaker matchmaker;
    private final ProxyManager proxyManager;
    private final Javalin javalin;

    public WebServer(ServerRegistry registry, ProxyManager proxyManager) {
        javalin = Javalin.create(config -> {
            config.registerPlugin(new OpenApiPlugin(openApiPluginConfiguration -> {
                openApiPluginConfiguration
                        .withDefinitionConfiguration((version, openApiDefinition) ->
                                openApiDefinition.withInfo(openApiInfo -> {
                                    OpenApiContact openApiContact = new OpenApiContact();
                                    openApiContact.setName("Adam Matthew");
                                    openApiContact.setEmail("mrpotato101@nethergames.org");

                                    openApiInfo
                                            .title("GSMS - Centralized Moderation Endpoint")
                                            .description("GSMS (Game Server Management System) Web API")
                                            .version("1.2.0")
                                            .setContact(openApiContact);
                                })
                        );
            }));

            config.registerPlugin(new ReDocPlugin());
        }).exception(Exception.class, JavalinUtil::handleException);

        this.registry = registry;
        this.proxyManager = proxyManager;
        this.matchmaker = new Matchmaker(registry, proxyManager);

        javalin.post("/status", this::reportStatus);
        javalin.get("/status/{instanceName}", this::getStatus);
        javalin.get("/status", this::allStatusReports);
        javalin.post("/register", this::registerServer);
        javalin.post("/matchmake", this::matchmake);
        javalin.post("/state", this::updateState);
        javalin.get("/type/{serverType}", this::getTypes);
        javalin.get("/total", this::getTotal);
        javalin.get("/stats/region", this::getRegionStats);
        javalin.get("/global", this::globalPlayerCount);
        javalin.get("/type/{serverType}/{gameType}", this::getTypesAndGameType);
        javalin.post("/proxy", this::registerProxy);
        javalin.patch("/proxy", this::updateProxyStatus);
    }

    public void start(int port) {
        javalin.start(port);
    }

    public void shutdown() {
        javalin.stop();
    }

    @OpenApi(
            path = "/proxy",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = ProxyModel.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200"),
                    @OpenApiResponse(status = "500")
            }
    )
    public void registerProxy(Context context) {
        ProxyModel model = context.bodyAsClass(ProxyModel.class);

        context.status(this.proxyManager.registerProxy(model) ? 200 : 500);
    }

    @OpenApi(
            path = "/proxy",
            methods = HttpMethod.PATCH,
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = ProxyStatusBody.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200", description = "Returned if the status update was successful"),
                    @OpenApiResponse(status = "500", description = "Returned if the server with the proxyId was not found.")
            }
    )
    public void updateProxyStatus(Context context) {
        ProxyStatusBody statusBody = context.bodyAsClass(ProxyStatusBody.class);

        context.status(this.proxyManager.updateProxy(statusBody) ? 200 : 500);
    }

    @OpenApi(
            path = "/type/{serverType}/{gameType}",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = GameServerCounter.class))
            }
    )
    public void getTypesAndGameType(Context context) {
        String serverType = context.pathParam("serverType");
        String gameType = context.pathParam("gameType");

        HashMap<String, GameServerCounter> map = new HashMap<>();

        for (ServerModel model : this.registry.getInternalMap().values()) {

            if (model.getGameType().equals(gameType) && model.getServerType().equals(serverType)) {
                GameServerCounter counter = new GameServerCounter();
                counter.setCount(model.getPlayerCount());
                counter.setMax(model.getMaxPlayerCount());
                map.put(model.getServerUniqueId(), counter);
            }
        }

        context.json(map);
    }

    @OpenApi(
            path = "/matchmake",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = MatchmakingRequest.class)),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = MatchmakerResult.class))
            }
    )
    public void matchmake(Context context) {
        MatchmakingRequest request = context.bodyAsClass(MatchmakingRequest.class);

        try {
            context.json(matchmaker.matchmake(request, false));
        } catch (Throwable t) {
            log.error("Error", t);
        }

        this.registry.getMetricsManager().increaseMatchmakerRequests();
    }

    @OpenApi(
            path = "/total",
            methods = HttpMethod.GET
    )
    public void getTotal(Context context) {
        HashMap<String, HashMap<String, Object>> map = new HashMap<>();

        for (ServerModel model : this.registry.getInternalMap().values()) {

            HashMap<String, Object> typeMap = map.get(model.getServerType());
            if (typeMap == null) {
                typeMap = new HashMap<>() {{
                    put("count", model.getPlayerCount());
                    put("max", model.getMaxPlayerCount());
                }};
                map.put(model.getServerType(), typeMap);
            } else {
                int currentCount = (int) typeMap.get("count");
                int maxCount = (int) typeMap.get("max");

                typeMap.replace("count", currentCount + model.getPlayerCount());
                typeMap.replace("max", maxCount + model.getMaxPlayerCount());
            }

            if (!model.getGameType().equals("")) {
                HashMap<String, Integer> gameTypeMap = (HashMap<String, Integer>) typeMap.get(model.getGameType());
                if (gameTypeMap == null) {
                    gameTypeMap = new HashMap<>() {{
                        put("count", model.getPlayerCount());
                        put("max", model.getMaxPlayerCount());
                    }};
                    typeMap.put(model.getGameType(), gameTypeMap);
                } else {
                    int currentTypeCount = gameTypeMap.get("count");
                    int currentMaxCount = gameTypeMap.get("max");

                    gameTypeMap.replace("count", currentTypeCount + model.getPlayerCount());
                    gameTypeMap.replace("max", currentMaxCount + model.getMaxPlayerCount());
                }
            }
        }

        context.json(map);
    }

    @OpenApi(
            path = "/type/{serverType}",
            methods = HttpMethod.GET,
            description = "Gets all the registered servers of the type serverType",
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = GameServerCounter.class))
            }
    )
    public void getTypes(Context context) {
        String type = context.pathParam("serverType");
        HashMap<String, GameServerCounter> list = new HashMap<>();

        for (ServerModel model : this.registry.getInternalMap().values()) {
            if (model.getServerType().equals(type)) {
                GameServerCounter counter = new GameServerCounter();
                counter.setCount(model.getPlayerCount());
                counter.setMax(model.getMaxPlayerCount());
                list.put(model.getServerUniqueId(), counter);
            }
        }

        context.json(list);
    }

    @OpenApi(
            path = "/state",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = StateUpdateRequest.class)),
            description = "Updates a gameservers queueing / touch queueing state.",
            responses = {
                    @OpenApiResponse(status = "200", description = "The state was updated successfully"),
                    @OpenApiResponse(status = "500", description = "Returned if no game server with the given serverUniqueId was found")
            }
    )
    public void updateState(Context context) {
        StateUpdateRequest model = context.bodyAsClass(StateUpdateRequest.class);

        if (this.registry.updateState(model)) {
            context.status(200);
        } else {
            context.status(500);
        }
    }

    @OpenApi(
            path = "/register",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = ServerModel.class)),
            responses = {
                    @OpenApiResponse(status = "200"),
                    @OpenApiResponse(status = "500", description = "Returned when a server with the same serverUniqueId already exists")
            }
    )
    public void registerServer(Context ctx) {
        ServerModel model = ctx.bodyAsClass(ServerModel.class);
        ServerUniqueId uniqueId = ServerUniqueId.fromString(model.getServerUniqueId());
        model.setRegion(uniqueId.getRegion());
        model.setServerType(uniqueId.getServerType());
        model.setGameType(uniqueId.getGameType());
        model.setLastSuccessfulPush(System.currentTimeMillis());
        if (uniqueId.getGameType().isEmpty()) {
            model.setDeploymentName(uniqueId.getRegion() + "-" + uniqueId.getServerType());
        } else {
            model.setDeploymentName(uniqueId.getRegion() + "-" + uniqueId.getServerType() + "-" + uniqueId.getGameType());
        }

        if (this.registry.register(model)) {
            ctx.status(200);
        } else {
            ctx.status(500);
        }
    }

    @OpenApi(
            path = "/stats/region",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200")
            }
    )
    public void getRegionStats(Context context) {
        Map<String, RegionStatus> result = new HashMap<>();
        for (Region region : Region.values()) {
            RegionStatus status = new RegionStatus(proxyManager.getLastRegionCounts().get(region.name()), proxyManager.isRegionActive(region));
            result.put(region.name(), status);
        }
        context.json(result);
    }

    @OpenApi(
            path = "/status",
            methods = HttpMethod.POST,
            description = "Pushes the status of a game server to the system.",
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = StatusBody.class)),
            responses = {
                    @OpenApiResponse(status = "200", description = "Returned when the inactivity timer was updated successfully and the update has been handled"),
                    @OpenApiResponse(status = "500", description = "Returned when the given server was previously removed due to inactivity. Can be used to re-register to the Matchmaker.")
            }
    )
    public void reportStatus(Context context) {
        StatusBody model = context.bodyAsClass(StatusBody.class);

        if (model != null) {
            if (this.registry.updateStatus(model)) {
                context.status(200);
            } else {
                context.status(500);
            }
        }
    }

    @OpenApi(
            path = "/status/{instanceName}",
            methods = HttpMethod.GET,
            description = "Returns the RegisteredServerEntry of the requested game server, or 404 if the requested id could not be found.",
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServerModel.class)),
                    @OpenApiResponse(status = "404")
            }
    )
    public void getStatus(Context context) {
        String instanceName = context.pathParam("instanceName");

        ServerModel entry = this.registry.getInternalMap().get(instanceName);
        if (entry != null) {
            context.json(entry);
        } else {
            context.status(404);
        }
    }

    @OpenApi(
            path = "/status",
            methods = HttpMethod.GET,
            description = "Returns a list of all registered servers",
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServerModel.class)),
            }
    )
    public void allStatusReports(Context context) {
        context.json(this.registry.getInternalMap().values());
    }

    @OpenApi(
            path = "/global",
            methods = HttpMethod.GET,
            description = "Get the total amount of players online on the network",
            responses = {
                    @OpenApiResponse(status = "200")
            }
    )
    public void globalPlayerCount(Context context) {
        context.json(Collections.singletonMap("global", this.proxyManager.getGlobalPlayerCount()));
    }
}
