package org.nethergames.gsms.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.nethergames.gsms.data.model.*;
import org.nethergames.microcommon.CommonClient;
import org.nethergames.microcommon.request.RequestResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class GSMSClient extends CommonClient {
    public GSMSClient(String apiRoot) {
        super(apiRoot);
    }

    public RequestResponse<Void> registerProxy(ProxyModel model) throws IOException {
        return runPost("/proxy", model, new TypeReference<Void>() {});
    }

    public RequestResponse<Void> updateProxyStatus(ProxyStatusBody statusBody) throws IOException {
        return runPatch("/proxy", statusBody, new TypeReference<Void>() {});
    }

    public RequestResponse<Map<String, GameServerCounter>> getTypesAndGameType(String serverType, String gameType) throws IOException {
        return runGet("/type/" + serverType + "/" + gameType, new TypeReference<Map<String, GameServerCounter>>() {});
    }

    public RequestResponse<MatchmakerResult> matchmake(MatchmakingRequest request) throws IOException {
        return runPost("/matchmake", request, new TypeReference<MatchmakerResult>() {});
    }

    public RequestResponse<Map<String, GameServerCounter>> getTypes(String serverType) throws IOException {
        return runGet("/type/" + serverType, new TypeReference<Map<String, GameServerCounter>>() {});
    }

    public RequestResponse<Void> updateStatus(StateUpdateRequest request) throws IOException {
        return runPost("/state", request, new TypeReference<Void>() {});
    }

    public RequestResponse<Void> registerServer(ServerModel model) throws IOException {
        return runPost("/register", model, new TypeReference<Void>() {});
    }

    public RequestResponse<Map<String, RegionStatus>> getRegionStats() throws IOException {
        return runGet("/stats/region", new TypeReference<Map<String, RegionStatus>>() {});
    }

    public RequestResponse<Void> updateStatus(StatusBody statusBody) throws IOException {
        return runPost("/status", statusBody, new TypeReference<>() {
        });
    }

    public RequestResponse<ServerModel> getStatus(String instanceName) throws IOException {
        return runGet("/status/" + instanceName, new TypeReference<>() {
        });
    }

    public RequestResponse<List<ServerModel>> getAllStatusReports() throws IOException {
        return runGet("/status", new TypeReference<List<ServerModel>>() {
        });
    }

    public RequestResponse<Map<String, Integer>> getGlobalPlayerCount() throws IOException {
        return runGet("/global", new TypeReference<Map<String, Integer>>() {
        });
    }

    public RequestResponse<Void> pushDataPoints(List<FluxDataPoint> dataPoints) throws IOException {
        return runPost("/metric", dataPoints, new TypeReference<Void>() {});
    }
}