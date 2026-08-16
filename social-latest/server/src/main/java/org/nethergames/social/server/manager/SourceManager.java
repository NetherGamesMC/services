package org.nethergames.social.server.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.nethergames.social.server.Social;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class SourceManager {

    private static final long TIMEOUT_CHECK_INTERVAL = 15; // every 10 seconds + 5 toleration
    private final Logger logger = LoggerFactory.getLogger("SourceManager");
    private final Map<String, String> sources = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public String signupSource(String name) {
        String sourceId = UUID.randomUUID().toString();

        if (sources.containsKey(sourceId)) {
            return signupSource(name); // very rare case: sourceId that was generated is taken
        } else {

            String previousSource = getFromName(name);
            if (previousSource != null) {
                // already exists, remove old
                Social.getInstance().getLocalityManager().invalidateSourceEntries(previousSource);
                sources.remove(previousSource);
                logger.info("Removed players for restarted node {}, previous id: {}", name, previousSource);
            }

            sources.put(sourceId, name);

            logger.info("Signed up new source {} with sourceId {}", name, sourceId);
        }


        return sourceId;
    }

    public String getFromName(String name) {
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            if (entry.getValue().equals(name)) return entry.getKey();
        }

        return null;
    }

    public void removeSource(String sourceId) {

        sources.remove(sourceId);
        Social.getInstance().getLocalityManager().invalidateSourceEntries(sourceId);
        logger.warn("Removed source {}", sourceId);
    }

    public boolean sourceExists(String sourceId) {
        return this.sources.containsKey(sourceId);
    }

    @Data
    @AllArgsConstructor
    private static class Entry {
        private boolean heartbeatReceived;
        private ScheduledFuture<?> scheduledFuture;
    }
}
