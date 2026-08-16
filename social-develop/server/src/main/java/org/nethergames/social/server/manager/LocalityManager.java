package org.nethergames.social.server.manager;

import com.google.common.eventbus.Subscribe;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.nethergames.social.data.request.locality.LocalityEntry;
import org.nethergames.social.rpc.PlayerEvent;
import org.nethergames.social.rpc.PlayerStatus;
import org.nethergames.social.server.Social;
import org.nethergames.social.server.events.PlayerConnectedEvent;
import org.nethergames.social.server.events.PlayerDisconnectEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class LocalityManager {
    private final Map<String, LocalityEntry> storageMap = new ConcurrentHashMap<>();
    private final List<StreamObserver<PlayerEvent>> eventListeners = new ArrayList<>();
    private final Object object = new Object();

    public void addEventListener(StreamObserver<PlayerEvent> observer) {
        synchronized (object) {
            eventListeners.add(observer);
        }
    }

    public void addPlayerStatus(LocalityEntry entry) {
        if (!Social.getInstance().getSourceManager().sourceExists(entry.getSourceUid()))
            throw new IllegalArgumentException("Source " + entry.getSourceUid() + " does not exist.");

        if (entry.getLocation().isEmpty()) {
            Social.getInstance().getLogger().info("Added player {} on proxy {}.", entry.getPlayerIdentifier(), entry.getProxyId());
        } else {
            Social.getInstance().getLogger().info("Added player {} to server {} on proxy {}.", entry.getPlayerIdentifier(), entry.getLocation(), entry.getProxyId());
        }

        storageMap.put(entry.getPlayerIdentifier(), entry);

        Social.getInstance().getEventBus().post(new PlayerConnectedEvent(entry.getPlayerIdentifier(), entry.getPlayerName()));
    }

    public Map<String, LocalityEntry> getFullStatusBatch(Set<String> identifiers) {
        Map<String, LocalityEntry> result = new LinkedHashMap<>();
        identifiers.forEach(identifier -> result.put(identifier, storageMap.get(identifier)));
        return result;
    }

    public Map<String, Boolean> getStatusBatch(Set<String> identifiers) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        identifiers.forEach(identifier -> result.put(identifier, storageMap.containsKey(identifier)));

        return result;
    }

    public boolean isOnline(String playerIdentifier) {
        return storageMap.containsKey(playerIdentifier);
    }

    public boolean removePlayer(String playerIdentifier) {
        LocalityEntry entry = storageMap.remove(playerIdentifier);
        if (entry != null) {
            Social.getInstance().getEventBus().post(new PlayerDisconnectEvent(playerIdentifier, entry.getPlayerName()));
            storageMap.remove(entry.getPlayerName());
        }

        return entry != null;
    }

    public String getServerOf(String playerIdentifier) {
        LocalityEntry entry = storageMap.get(playerIdentifier);

        if (entry != null) {
            return entry.getLocation();
        }

        return null;
    }

    public void invalidateSourceEntries(String sourceId) {
        for (Map.Entry<String, LocalityEntry> entry : storageMap.entrySet()) {
            if (entry.getValue().getSourceUid().equals(sourceId)) {
                storageMap.remove(entry.getKey());
            }
        }
    }

    public LocalityEntry getPlayerByXuid(String playerIdentifier) {
        return storageMap.get(playerIdentifier);
    }

    public LocalityEntry getPlayerByName(String playerName) {
        return storageMap.values().stream().filter((entry) -> entry.getPlayerName().equalsIgnoreCase(playerName)).findFirst().orElse(null);
    }

    public void invalidateAll() {
        storageMap.clear();
    }

    public Map<String, LocalityEntry> getAll() {
        return storageMap;
    }

    public int getSize() {
        return storageMap.size();
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        synchronized (object) {
            publishPlayerEvent(event.getXuid(), event.getPlayerName(), true);
        }
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectEvent event) {
        synchronized (object) {
            publishPlayerEvent(event.getXuid(), event.getPlayerName(), false);
        }
    }

    private void publishPlayerEvent(String xuid, String playerName, boolean isConnected) {
        var iter = eventListeners.iterator();

        while (iter.hasNext()) {
            var eventConsumers = iter.next();

            try {
                var builder = PlayerEvent.newBuilder();
                builder.setPlayerXuid(xuid);
                builder.setPlayerName(playerName);
                builder.setStatus(isConnected ? PlayerStatus.Online : PlayerStatus.Disconnected);

                eventConsumers.onNext(builder.build());
            } catch (Throwable t) {
                if (Status.fromThrowable(t).getCode() != Status.Code.CANCELLED) {
                    log.error("Received an error", t);
                }

                iter.remove();
                eventConsumers.onCompleted();
            }
        }
    }
}
