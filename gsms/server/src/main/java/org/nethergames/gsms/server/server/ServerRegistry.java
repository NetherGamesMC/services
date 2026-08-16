package org.nethergames.gsms.server.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import okhttp3.Request.Builder;
import org.jetbrains.annotations.NotNull;
import org.nethergames.gsms.data.Region;
import org.nethergames.gsms.data.model.ServerModel;
import org.nethergames.gsms.data.model.ServerModel.Status;
import org.nethergames.gsms.data.model.StateUpdateRequest;
import org.nethergames.gsms.data.model.StatusBody;
import org.nethergames.gsms.rpc.ServerEventType;
import org.nethergames.gsms.server.GSMS;
import org.nethergames.gsms.server.metrics.MetricsManager;
import org.nethergames.gsms.server.scheduler.RegionActivationDeterminator;
import org.nethergames.gsms.server.scheduler.ServerTaskExecutor;
import org.nethergames.utils.server.ServerUniqueId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Getter
@Log4j2(topic = "ServerRegistry")
public class ServerRegistry {
    private static final String LEGACY_REGISTRY = System.getenv("LEGACY_REGISTRY");
    private static final String POD_NAMESPACE = System.getenv("POD_NAMESPACE");

    private final ConcurrentHashMap<String, ServerModel> internalMap;
    private final ConcurrentHashMap<String, String> podToIpMap;

    // Region mapped tree set, we want to sort all server models by player count.
    private final Object objectLock = new Object();
    private final List<ServerModel> globalLobbySelector = new ArrayList<>();
    private final Map<Region, List<ServerModel>> lobbySelector = Map.of(
            Region.AP, new ArrayList<>(),
            Region.EU, new ArrayList<>(),
            Region.US, new ArrayList<>(),
            Region.IND, new ArrayList<>()
    );

    private final long timeoutDiff;
    private AtomicInteger pendingRequests = new AtomicInteger(0);

    private KubernetesClient client = new KubernetesClientBuilder().build();
    private SharedIndexInformer<Pod> executor = null;
    private OkHttpClient httpClient;

    private final GSMS server;
    private final MetricsManager metricsManager;
    private final ObjectMapper objectMapper;
    private final KeyLockManager lockManager; // Provide concurrency safety.

    public ServerRegistry(int entryTimeout, int maxAttempts, GSMS server) {
        log.info("Starting Server Registry");

        this.server = server;
        internalMap = new ConcurrentHashMap<>();
        podToIpMap = new ConcurrentHashMap<>();
        objectMapper = new ObjectMapper();
        metricsManager = new MetricsManager(this, server.getProxyManager());
        lockManager = KeyLockManagers.newLock(10, TimeUnit.SECONDS);

        timeoutDiff = TimeUnit.SECONDS.toMillis((long) entryTimeout * maxAttempts);

        ServerTaskExecutor.scheduleRepeating(new RegionActivationDeterminator(server.getProxyManager()), 0, 10, TimeUnit.SECONDS);
    }

    public void initRegistry() {
        if (LEGACY_REGISTRY != null) {
            ServerTaskExecutor.scheduleRepeating(this::unregisterOldServers, 3, 3, TimeUnit.SECONDS);
            ServerTaskExecutor.scheduleRepeating(() -> getServer().getProxyManager().updateProxiesLobby(), 3, 3, TimeUnit.SECONDS);
        } else {
            ServerTaskExecutor.scheduleRepeating(this::scrapePodMetrics, 8, 8, TimeUnit.SECONDS);

            httpClient = (new OkHttpClient.Builder()).connectionPool(new ConnectionPool(20, 5L, TimeUnit.MINUTES))
                    .callTimeout(10L, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            executor = client.pods().inNamespace(POD_NAMESPACE).inform(new ResourceEventHandler<>() {
                @Override
                public void onAdd(Pod pod) {
                    try {
                        updatePod(pod);
                    } catch (Throwable err) {
                        log.throwing(err);
                    }
                }

                @Override
                public void onUpdate(Pod oldPod, Pod pod) {
                    if (!oldPod.getMetadata().getResourceVersion().equals(pod.getMetadata().getResourceVersion())) {
                        try {
                            updatePod(pod);
                        } catch (Throwable err) {
                            log.throwing(err);
                        }
                    }
                }

                @Override
                public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {
                    var podName = ServerUniqueId.fromString(pod.getMetadata().getName()).toString();

                    lockManager.executeLocked(podName, () -> {
                        if (unregister(podName, true)) {
                            log.info("Pod {} has been removed from GSMS", podName);
                        }
                    });
                }
            }, 5 * 1000L); // 5 seconds
        }
    }

    public boolean register(ServerModel model) {
        if (!internalMap.containsKey(model.getServerUniqueId())) {
            internalMap.put(model.getServerUniqueId(), model);

            // Publish the update to all listeners.
            var service = server.getGrpcServer().getGameService();
            service.publishServerEvent(model, ServerEventType.SERVER_ADD);
            service.publishActiveEvent(model.getGameType(), model.getServerType());

            // Then we add the server into lobby selector list.
            if (model.getServerType().equalsIgnoreCase("lobby")) {
                lobbySelector.get(model.getServerRegion()).add(model);
                globalLobbySelector.add(model);

                log.info("Registered {} into global lobby selector", model.getServerUniqueId());
            }

            log.info("Heartbeat received for {} ({}/{}) in region {}", model.getServerUniqueId(), model.getServerType(), model.getGameType(), model.getRegion());

            return true;
        }

        return false;
    }

    public boolean unregister(String serverUniqueId, boolean removePodIp) {
        boolean update = false;

        if (internalMap.containsKey(serverUniqueId)) {
            var model = internalMap.remove(serverUniqueId);

            // Publish the update to all listeners.
            server.getGrpcServer().getGameService().publishServerEvent(model, ServerEventType.SERVER_REMOVE);

            // Then we remove the server from lobby selector list.
            removeLobbyModel(model);

            update = true;
        }

        if (removePodIp && podToIpMap.containsKey(serverUniqueId)) {
            podToIpMap.remove(serverUniqueId);
            update = true;

            log.info("Removed server {} from server list.", serverUniqueId);
        }

        return update;
    }

    public Collection<ServerModel> allServersWith(String serverType, String gameType) {
        ArrayList<ServerModel> list = new ArrayList<>();

        for (ServerModel model : internalMap.values()) {
            if (model.getServerType().equals(serverType) && model.getGameType().equals(gameType)) {
                list.add(model);
            }
        }
        return list;
    }

    public boolean handlePodUpdate(String podName, StatusBody status) {
        if (!podToIpMap.containsKey(podName)) {
            return false;
        }

        var ip = podToIpMap.get(podName);
        var model = internalMap.get(podName);
        var uniqueId = ServerUniqueId.fromString(podName);

        if (model == null) {
            model = new ServerModel();

            model.setServerUniqueId(podName);
            model.setRegion(uniqueId.getRegion());
            model.setServerType(uniqueId.getServerType());
            model.setGameType(uniqueId.getGameType());
            if (uniqueId.getGameType().isEmpty()) {
                model.setDeploymentName(uniqueId.getRegion() + "-" + uniqueId.getServerType());
            } else {
                model.setDeploymentName(uniqueId.getRegion() + "-" + uniqueId.getServerType() + "-" + uniqueId.getGameType());
            }

            register(model.applyUpdate(status, ip, 19132));
        } else {
            // Check if the pod is terminating, if so then remove the lobby from the server.
            if (model.getStatus() == Status.RUNNING && status.getStatus() == Status.TERMINATING) {
                removeLobbyModel(model);
            }

            var compare = model.compareChanges(status);

            model.applyUpdate(status, ip, 19132);

            // Publish events only if there is a change in player count.
            if (compare) {
                var service = server.getGrpcServer().getGameService();
                service.publishServerEvent(model, ServerEventType.SERVER_UPDATE);
                service.publishActiveEvent(model.getGameType(), model.getServerType());
            }
        }

        if (model.getServerType().equalsIgnoreCase("lobby")) {
            synchronized (objectLock) {
                globalLobbySelector.sort(ServerModel::compareTo);
                lobbySelector.values().forEach(o -> o.sort(ServerModel::compareTo));
            }

            getServer().getProxyManager().updateProxiesLobby();
        }

        return true;
    }

    public boolean updateStatus(StatusBody body) {
        ServerModel model = internalMap.get(body.getServerUniqueId());
        if (model != null) {
            model.setPlayerCount(body.getPlayerCount());
            model.setLastTps(body.getTps());
            model.setStatus(body.getStatus());
            model.setLastUsage(body.getUsage());
            model.setLastMemoryUsage(body.getMemoryUsage());
            model.setLastSuccessfulPush(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public boolean updateState(StateUpdateRequest request) {
        ServerModel entry = internalMap.get(request.getServerUniqueId());
        if (entry != null) {
            entry.setQueueingState(request.isQueueing());
            entry.setTouchOnlyState(request.isTouchQueueing());
            return true;
        }

        return false;
    }

    public void shutdown() {
        log.info("Shutting down kubernetes game pods management and scheduler.");

        client.close();

        if (httpClient != null) {
            httpClient.connectionPool().evictAll();
        }

        if (executor != null) {
            executor.close();
        }

        getMetricsManager().shutdown();
    }

    private void updatePod(Pod pod) {
        var podIp = pod.getStatus().getPodIP();
        var podName = ServerUniqueId.fromString(pod.getMetadata().getName()).toString();

        var serverInstance = podToIpMap.get(podName);
        var isTerminating = pod.getMetadata().getDeletionTimestamp() != null;

        if (podIp == null) {
            return;
        }

        if (isTerminating) {
            lockManager.executeLocked(podName, () -> {
                var model = internalMap.get(podName);

                if (model != null && model.getStatus() == Status.RUNNING) {
                    removeLobbyModel(model);

                    model.setStatus(Status.TERMINATING);

                    log.info("Pod {} is now in terminating state.", podName);
                }
            });
        } else {
            podToIpMap.put(podName, podIp);

            if (serverInstance == null) {
                log.info("Registered pod {} with assigned IP of {}", podName, podIp);
            } else if (!serverInstance.equals(podIp)) {
                log.info("Replaced pod {} with assigned IP of {}", podName, podIp);
            } else {
                return;
            }

            requestPodMetadata(podName, podIp, true);
        }
    }

    private void removeLobbyModel(ServerModel model) {
        lobbySelector.get(model.getServerRegion()).remove(model);

        if (globalLobbySelector.remove(model)) {
            getServer().getProxyManager().updateProxiesLobby();

            log.info("Unregistered {} into global lobby selector", model.getServerUniqueId());
        }
    }

    private void scrapePodMetrics() {
        if (pendingRequests.get() > 0) {
            return;
        }

        pendingRequests.set(0);
        podToIpMap.forEach((podName, ip) -> {
            pendingRequests.incrementAndGet();

            requestPodMetadata(podName, ip, false);
        });
    }

    private void requestPodMetadata(String podName, String ip, boolean lockDecrement) {
        httpClient.newCall((new Builder()).get().url("http://" + ip + ":8080/metrics").build()).enqueue(new Callback() {
            private final AtomicBoolean decrementLock = new AtomicBoolean(lockDecrement);

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (decrementLock.compareAndSet(false, true)) {
                    pendingRequests.decrementAndGet();
                }

                lockManager.executeLocked(podName, () -> {
                    try {
                        handlePodTimeout(podName);
                    } catch (Throwable err) {
                        log.throwing(err);
                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (decrementLock.compareAndSet(false, true)) {
                    pendingRequests.decrementAndGet();
                }

                if (response.body() == null) {
                    log.warn("{} response is {} and has no body", podName, response.code());
                    return;
                }

                try {
                    var status = objectMapper.readValue(response.body().bytes(), new TypeReference<StatusBody>() {});
                    if (!status.isUsingGrpc()) {
                        lockManager.executeLocked(podName, () -> handlePodUpdate(podName, status));
                    }
                } catch (Throwable err) {
                    log.throwing(err);
                }
            }
        });
    }

    private void handlePodTimeout(String podName) {
        if (!internalMap.containsKey(podName) || !internalMap.get(podName).isTimeout()) {
            return;
        }

        if (unregister(podName, false)) {
            log.error("Failed to scrape pod metadata for {}, pod has been disabled", podName);
        }
    }

    private void unregisterOldServers() {
        long timeDiff = System.currentTimeMillis() - timeoutDiff;

        for (Map.Entry<String, ServerModel> entry : internalMap.entrySet()) {
            if (timeDiff <= entry.getValue().getLastSuccessfulPush()) {
                continue;
            }

            if (unregister(entry.getKey(), true)) {
                log.warn("Removed {} due to missing status reports.", entry.getKey());
            }
        }
    }
}
