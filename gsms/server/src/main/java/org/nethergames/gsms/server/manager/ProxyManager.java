package org.nethergames.gsms.server.manager;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.data.Region;
import org.nethergames.gsms.data.model.ProxyModel;
import org.nethergames.gsms.data.model.ProxyStatusBody;
import org.nethergames.gsms.data.model.ServerModel;
import org.nethergames.gsms.rpc.ServerEventType;
import org.nethergames.gsms.server.GSMS;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Proxy Manager class, handles timeouts for each proxy, and provide real-time lobby position update
 * to all regions.
 */
@Log4j2(topic = "ProxyManager")
public class ProxyManager {
    @Getter
    private final GSMS server;
    @Getter
    private final Map<String, Integer> lastRegionCounts = new ConcurrentHashMap<>();

    private final EnumMap<Region, Boolean> regionActivationMap = new EnumMap<>(Region.class);
    private final Map<String, ProxyModel> internalMap = new HashMap<>();
    private final Map<Region, ServerModel> currentLobbySelection = new HashMap<>();

    @Setter
    @Getter
    private int currentGlobalCount = 0;

    public ProxyManager(GSMS server) {
        this.server = server;

        for (Region region : Region.values()) {
            regionActivationMap.put(region, true);
            lastRegionCounts.put(region.name(), 0);
        }
    }

    public synchronized void updateProxiesLobby() {
        var svc = getServer().getGrpcServer();

        for (Region region : Region.values()) {
            var lobby = getLobbySelectionFor(region);

            // Ignore if the lobby was not found (Possibly that there is no lobbies available).
            if (lobby == null) {
                continue;
            }

            // Ignore the currently selected lobby.
            if (currentLobbySelection.containsKey(region) && currentLobbySelection.get(region).equals(lobby)) {
                continue;
            }

            // Broadcast the update to all servers.
            getProxiesInRegion(region).forEach(model -> svc.getGameService().publishServerEvent(model.getProxyId(), lobby, ServerEventType.SERVER_LOBBY_UPDATE));

            // Put the server into current selection lobby.
            currentLobbySelection.put(region, lobby);
        }
    }

    public boolean registerProxy(ProxyModel model) {
        initialBroadcast(model);

        if (!internalMap.containsKey(model.getProxyId())) {
            internalMap.put(model.getProxyId(), model);

            return true;
        }
        return false;
    }

    public boolean unregisterProxy(ProxyModel model) {
        if (internalMap.containsKey(model.getProxyId())) {
            internalMap.remove(model.getProxyId());
            return true;
        }
        return false;
    }

    public Map<String, Integer> getRegionNumbers() {
        HashMap<String, Integer> map = new HashMap<>();
        for (Region r : Region.values()) {
            map.put(r.name(), 0);
        }

        for (ProxyModel model : internalMap.values()) {
            map.replace(model.getRegion(), map.get(model.getRegion()) + model.getPlayerCount());
        }

        return map;
    }

    public int getGlobalPlayerCount() {
        return sum(getRegionNumbers(), Integer::sum);
    }

    public boolean updateProxy(ProxyStatusBody statusBody) {
        return updateProxy(statusBody.getProxyId(), statusBody.getPlayerCount());
    }

    public boolean updateProxy(String proxyId, int playerCount) {
        ProxyModel model = internalMap.get(proxyId);
        if (model != null) {
            model.setPlayerCount(playerCount);
            return true;
        }
        return false;
    }

    public List<ProxyModel> getProxiesInRegion(Region region) {
        return internalMap.values().stream().filter(o -> o.getServerRegion() == region).collect(Collectors.toList());
    }

    public void setRegionActivation(Region region, boolean val) {
        regionActivationMap.replace(region, val);
    }

    public boolean allRegionsDisabled() {
        return Stream.of(Region.values()).noneMatch(this::isRegionActive);
    }

    public boolean isRegionActive(Region region) {
        return regionActivationMap.get(region);
    }

    /**
     * Broadcast all related information to the proxy before initiating events.
     *
     * @param model The proxy model itself.
     */
    private void initialBroadcast(ProxyModel model) {
        var svc = getServer().getGrpcServer();
        var serverRegistry = getServer().getServerRegistry();

        serverRegistry.getInternalMap().forEach((uniqueId, serverModel) -> svc.getGameService().publishServerEvent(model.getProxyId(), serverModel, ServerEventType.SERVER_ADD));

        var serverModel = currentLobbySelection.get(model.getServerRegion());
        if (serverModel == null) {
            serverModel = getLobbySelectionFor(model.getServerRegion());
        }

        if (serverModel == null) {
            return;
        }

        svc.getGameService().publishServerEvent(model.getProxyId(), serverModel, ServerEventType.SERVER_LOBBY_UPDATE);
    }

    private ServerModel getLobbySelectionFor(Region region) {
        var serverRegistry = getServer().getServerRegistry();

        if (isRegionActive(region) && !serverRegistry.getLobbySelector().get(region).isEmpty()) {
            return serverRegistry.getLobbySelector().get(region).get(0);
        }

        if (serverRegistry.getGlobalLobbySelector().isEmpty()) {
            return null;
        }

        return serverRegistry.getGlobalLobbySelector().get(0);
    }

    private static int sum(Map<?, Integer> m, BinaryOperator<Integer> summer) {
        return m.values().stream().reduce(0, summer);
    }
}
