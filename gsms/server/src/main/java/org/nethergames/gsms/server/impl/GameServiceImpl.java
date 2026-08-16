package org.nethergames.gsms.server.impl;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.data.model.GameServerCounter;
import org.nethergames.gsms.data.model.ProxyModel;
import org.nethergames.gsms.data.model.StatusBody;
import org.nethergames.gsms.rpc.*;
import org.nethergames.gsms.server.GSMS;
import org.nethergames.gsms.server.server.ServerRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.nethergames.gsms.data.model.ServerModel.Status.RUNNING;
import static org.nethergames.gsms.data.model.ServerModel.Status.TERMINATING;
import static org.nethergames.gsms.rpc.ServerStatus.STATUS_ONLINE;
import static org.nethergames.gsms.rpc.ServerStatus.STATUS_TERMINATING;

@Log4j2(topic = "GameServiceGrpc")
public class GameServiceImpl extends GSMSServiceGrpc.GSMSServiceImplBase {

    private static final String MIN_LENGTH_SIZE = "Length of a serverUniqueId must be greater than 5.";
    private static final String INVALID_SERVER_ID = "The serverUniqueId must be static and cannot be changed.";
    private static final String POD_IP_NOT_REGISTERED = "The pod is not registered to the GSMS internal map.";

    private final Object objectLock = new Object();

    private final ConcurrentHashMap<String, StreamObserver<ServerEvent>> activeListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StreamObserver<GameEvent>> activeEventsListener = new ConcurrentHashMap<>();
    private final GSMS server;

    public GameServiceImpl(GSMS server) {
        this.server = server;
    }

    /**
     * Publish server events to any subscribers available.
     *
     * @param serverModel The server model that is being published.
     * @param eventType   The type of event for the server model (update, add, delete).
     */
    public void publishServerEvent(org.nethergames.gsms.data.model.ServerModel serverModel, ServerEventType eventType) {
        activeListeners.forEach((id, stream) -> publishServerEvent(id, serverModel, eventType));
    }

    /**
     * Publish server model events to the given subscriber id.
     *
     * @param id          The id for the event to be published.
     * @param serverModel The server model that is being published.
     * @param eventType   The type of event for the server model (update, add, delete).
     */
    public void publishServerEvent(String id, org.nethergames.gsms.data.model.ServerModel serverModel, ServerEventType eventType) {
        if (!activeListeners.containsKey(id)) {
            return;
        }

        synchronized (objectLock) {
            var stream = activeListeners.get(id);

            try {
                ServerEvent.Builder builder = ServerEvent.newBuilder();
                builder.setEventType(eventType);
                builder.setServer(fromServerModel(serverModel));

                stream.onNext(builder.build());
            } catch (Throwable ignored) {
                // We let the handler for this stream to remove them from active listener.
            }
        }
    }

    public void publishAllServers(String id) {
        HashMap<String, List<String>> allServers = new HashMap<>();

        var registry = server.getServerRegistry();
        for (org.nethergames.gsms.data.model.ServerModel model : registry.getInternalMap().values()) {
            var list = allServers.computeIfAbsent(model.getGameType(), k -> new ArrayList<>());

            if (!list.contains(model.getServerType())) {
                list.add(model.getServerType());
            }
        }

        allServers.forEach((gameType, serverTypes) -> serverTypes.forEach(serverType -> publishActiveEvent(id, gameType, serverType)));
    }

    public void publishActiveEvent(String gameType, String serverType) {
        activeEventsListener.forEach((id, stream) -> publishActiveEvent(id, gameType, serverType));
    }

    public void publishActiveEvent(String id, String gameType, String serverType) {
        if (!activeEventsListener.containsKey(id)) {
            return;
        }

        synchronized (objectLock) {
            var stream = activeEventsListener.get(id);
            var registry = server.getServerRegistry();

            try {
                GameEvent.Builder builder = GameEvent.newBuilder();
                builder.setGameType(gameType);
                builder.setServerType(serverType);

                GameServerCounter counter = new GameServerCounter();
                for (org.nethergames.gsms.data.model.ServerModel model : registry.getInternalMap().values()) {
                    if (model.getGameType().equals(gameType) && model.getServerType().equals(serverType)) {
                        GameServerStatus.Builder status = GameServerStatus.newBuilder();

                        status.setServerUniqueId(model.getServerUniqueId());
                        status.setMaxPlayers(model.getMaxPlayerCount());
                        status.setTotalPlayers(model.getPlayerCount());

                        builder.addClusterData(status.build());

                        counter.setCount(counter.getCount() + model.getPlayerCount());
                        counter.setMax(counter.getMax() + model.getMaxPlayerCount());
                    }
                }

                builder.setTotalPlayers(counter.getCount());
                builder.setMaxPlayers(counter.getMax());

                stream.onNext(builder.build());
            } catch (Throwable ignored) {
                // We let the handler for this stream to remove them from active listener.
            }
        }
    }

    @Override
    public StreamObserver<UpdateModel> registerListenerGame(StreamObserver<GameEvent> listener) {
        return new StreamObserver<>() {
            private final ServerRegistry serverRegistry = server.getServerRegistry();
            private final AtomicBoolean isActive = new AtomicBoolean(true);

            private String serverUniqueId = null;

            @Override
            public void onNext(UpdateModel request) {
                if (!isActive.get()) {
                    return; // Prevent excess streams of data being sent when the client closed its connection.
                }

                try {
                    handleUpdateEvent(request);
                } catch (Throwable t) {
                    log.error("Unhandled error when handling received events.", t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (Status.fromThrowable(t).getCode() != Status.Code.CANCELLED) {
                    log.error("Received an error", t);
                }

                unregisterListener();
            }

            @Override
            public void onCompleted() {
                unregisterListener();
            }

            private void handleUpdateEvent(UpdateModel request) {
                try {
                    handleUpdateEvent0(request);
                } catch (Throwable t) {
                    if (Status.fromThrowable(t).getCode() != Status.Code.CANCELLED) {
                        log.error("Received an error", t);
                    }

                    unregisterListener();
                }
            }

            private void handleUpdateEvent0(UpdateModel request) {
                final String id = request.getServerUniqueId();
                if (id.length() < 5) {
                    listener.onError(Status.INVALID_ARGUMENT.withDescription(MIN_LENGTH_SIZE).asRuntimeException());
                    return;
                }

                if (serverUniqueId == null) {
                    serverUniqueId = id;
                } else if (!serverUniqueId.equalsIgnoreCase(id)) {
                    listener.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_SERVER_ID).asRuntimeException());
                    return;
                }

                if (!serverRegistry.handlePodUpdate(serverUniqueId, fromUpdateModel(request))) {
                    listener.onError(Status.NOT_FOUND.withDescription(POD_IP_NOT_REGISTERED).asRuntimeException());
                    return;
                }

                if (!activeEventsListener.containsKey(serverUniqueId)) {
                    log.info("Client connected: {}", serverUniqueId);

                    activeEventsListener.put(serverUniqueId, listener);

                    var map = serverRegistry.getInternalMap();
                    map.forEach((o, v) -> publishServerEvent(serverUniqueId, v, ServerEventType.SERVER_ADD));

                    publishAllServers(serverUniqueId);
                }
            }

            private void unregisterListener() {
                isActive.compareAndSet(true, false);

                if (serverUniqueId != null) {
                    log.info("Client disconnected: {}", serverUniqueId);

                    activeEventsListener.remove(serverUniqueId);
                    serverRegistry.unregister(serverUniqueId, false);

                    serverUniqueId = null;
                }

                listener.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<org.nethergames.gsms.rpc.ProxyModel> registerListenerProxy(StreamObserver<ServerEvent> listener) {
        return new StreamObserver<>() {
            private final AtomicBoolean firstLogin = new AtomicBoolean(false);
            private final AtomicBoolean isActive = new AtomicBoolean(true);

            private ProxyModel model = null;

            @Override
            public void onNext(org.nethergames.gsms.rpc.ProxyModel value) {
                if (!isActive.get()) {
                    return; // Prevent excess streams of data being sent when the client closed its connection.
                }

                try {
                    handleUpdateEvent(value);
                } catch (Throwable t) {
                    log.error("Unhandled error when handling received events.", t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (Status.fromThrowable(t).getCode() != Status.Code.CANCELLED) {
                    log.error("Received an error", t);
                }

                unregisterListener();
            }

            @Override
            public void onCompleted() {
                unregisterListener();
            }

            private void handleUpdateEvent(org.nethergames.gsms.rpc.ProxyModel value) {
                try {
                    handleUpdateEvent0(value);
                } catch (Throwable t) {
                    if (Status.fromThrowable(t).getCode() != Status.Code.CANCELLED) {
                        log.error("Received an error", t);
                    }

                    unregisterListener();
                }
            }

            private void handleUpdateEvent0(org.nethergames.gsms.rpc.ProxyModel value) {
                var proxyManager = server.getProxyManager();
                var proxyModel = new ProxyModel(value.getProxyId(), value.getRegion().name(), value.getPlayerCount());

                if (!activeListeners.containsKey(value.getProxyId())) {
                    activeListeners.put(value.getProxyId(), listener);

                    model = proxyModel;
                    firstLogin.set(true);
                }

                if (firstLogin.compareAndSet(true, false)) {
                    proxyManager.registerProxy(proxyModel);

                    log.info("Registered {} proxy with id {}", proxyModel.getRegion(), proxyModel.getProxyId());
                }

                proxyManager.updateProxy(value.getProxyId(), value.getPlayerCount());
            }

            private void unregisterListener() {
                isActive.compareAndSet(true, false);

                if (model != null) {
                    activeListeners.remove(model.getProxyId());
                    if (server.getProxyManager().unregisterProxy(model)) {
                        log.info("Unregistered {} proxy with id {}", model.getRegion(), model.getProxyId());
                    }

                    model = null;
                }

                listener.onCompleted();
            }
        };
    }

    public static StatusBody fromUpdateModel(UpdateModel model) {
        StatusBody body = new StatusBody();
        body.setServerUniqueId(model.getServerUniqueId());
        body.setPlayerCount(model.getStatus().getPlayerCount());
        body.setMaxPlayerCount(model.getStatus().getMaxPlayerCount());
        body.setQueueingState(model.getStatus().getQueueingState());
        body.setTouchOnlyState(model.getStatus().getTouchOnlyState());
        body.setTps(model.getStatus().getTps());
        body.setUsage(model.getStatus().getUsage());
        body.setMemoryUsage(model.getStatus().getMemoryUsage());
        body.setStatus(model.getStatus().getStatus() == STATUS_ONLINE ? RUNNING : TERMINATING);
        body.setProxyServerInfoType(model.getStatus().getProxyServerInfoType());

        return body;
    }

    public static Region fromRegionBase(org.nethergames.gsms.data.Region region) {
        return switch (region) {
            case AP -> Region.AP;
            case US -> Region.US;
            case EU -> Region.EU;
            default -> Region.IND;
        };
    }

    public static ServerModel fromServerModel(org.nethergames.gsms.data.model.ServerModel serverModel) {
        return ServerModel.newBuilder()
                .setRegion(fromRegionBase(serverModel.getServerRegion()))
                .setStatus(switch (serverModel.getStatus()) {
                    case RUNNING -> STATUS_ONLINE;
                    case TERMINATING -> STATUS_TERMINATING;
                })
                .setServerUniqueId(serverModel.getServerUniqueId())
                .setServerType(serverModel.getServerType())
                .setGameType(serverModel.getGameType())
                .setAddress(serverModel.getAddress())
                .setPort(serverModel.getPort())
                .setPlayerCount(serverModel.getPlayerCount())
                .setMaxPlayerCount(serverModel.getMaxPlayerCount())
                .setQueueingState(serverModel.isQueueingState())
                .setTouchOnlyState(serverModel.isTouchOnlyState())
                .setLastTps(serverModel.getLastTps())
                .setLastUsage(serverModel.getLastUsage())
                .setLastMemoryUsage(serverModel.getLastMemoryUsage())
                .setProxyServerInfoType(serverModel.getProxyServerInfoType())
                .build();
    }
}