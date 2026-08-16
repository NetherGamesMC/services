package org.nethergames.gsms.server.matchmaking;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.TransactionOptions;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.data.QueueingMode;
import org.nethergames.gsms.data.Region;
import org.nethergames.gsms.data.model.MatchmakerResult;
import org.nethergames.gsms.data.model.MatchmakerResult.ResultCode;
import org.nethergames.gsms.data.model.MatchmakingRequest;
import org.nethergames.gsms.data.model.ServerModel;
import org.nethergames.gsms.server.manager.ProxyManager;
import org.nethergames.gsms.server.server.ServerRegistry;

import java.util.*;

@Log4j2(topic = "Matchmaker")
public class Matchmaker {
    private final EnumSet<Region> REGION_WESTERN = EnumSet.of(Region.US, Region.EU);
    private final EnumSet<Region> REGION_ASIA = EnumSet.of(Region.AP, Region.IND);

    private final ServerRegistry registry;
    private final ProxyManager proxyManager;

    public Matchmaker(ServerRegistry registry, ProxyManager proxyManager) {
        this.registry = registry;
        this.proxyManager = proxyManager;
    }

    public Map<Region, MatchmakerResult> matchmakeLobbies() {
        var lobbies = new HashMap<Region, MatchmakerResult>();
        var matchmakerConfiguration = new MatchmakingRequest();
        matchmakerConfiguration.setServerType("lobby");
        matchmakerConfiguration.setGameType("");
        matchmakerConfiguration.setCanJoinFull(true);
        matchmakerConfiguration.setRegionQueueingEnabled(true);
        matchmakerConfiguration.setQueueingMode(QueueingMode.GLOBAL);

        for (Region region : Region.values()) {
            matchmakerConfiguration.setCurrentRegion(region.name());

            var options = new TransactionOptions();
            options.setBindToScope(true);

            ITransaction transaction = Sentry.startTransaction("matchmaking", "task", "Matchmaking run", options);
            try {
                lobbies.put(region, matchmake(matchmakerConfiguration, true));
            } catch (Throwable t) {
                transaction.setThrowable(t);
                transaction.setStatus(SpanStatus.INTERNAL_ERROR);

                log.error("Error occurred in lobbies matchmaking.", t);
            } finally {
                transaction.finish();
            }
        }

        return lobbies;
    }

    public MatchmakerResult matchmake(MatchmakingRequest request, boolean fallback) {
        // Check if all regions are inactive. If so we have to restart matchmaking request.
        if (proxyManager.allRegionsDisabled() && !fallback) {
            return matchmake(request, true);
        }

        MatchmakerResult result = new MatchmakerResult();
        result.setResponseId(UUID.randomUUID().toString());
        result.setResultCode(ResultCode.NONE_FOUND);

        // First stage is to check for region queuing, disable region queuing if it is from INDIA
        if (request.isRegionQueueingEnabled() && (!proxyManager.isRegionActive(request.getRegion()) || request.getRegion() == Region.IND)) {
            request.setRegionQueueingEnabled(false);
        }

        int fullServers = 0;
        int totalServers = 0;

        Collection<ServerModel> servers = registry.allServersWith(request.getServerType(), request.getGameType());

        servers.removeIf(model -> model.getStatus().equals(ServerModel.Status.TERMINATING));
        ArrayList<ServerModel> correctServers = new ArrayList<>();
        for (ServerModel model : servers) {
            if (fallback) {
                correctServers.add(model);
                continue;
            }

            if (request.isRegionQueueingEnabled()) {
                if (proxyManager.isRegionActive(model.getServerRegion()) && request.getRegion().equals(model.getServerRegion())) {
                    totalServers++;
                    if (model.getPlayerCount() < model.getMaxPlayerCount() || request.isCanJoinFull()) {
                        correctServers.add(model);
                    } else {
                        fullServers++;
                    }
                }
            } else {
                boolean sgActive = proxyManager.isRegionActive(Region.AP);
                boolean indActive = proxyManager.isRegionActive(Region.IND);

                // If the request was from ASIA, then check if any servers there is active for us to queue.
                // This condition will execute ONLY-IF SG and IND region is active.
                if (REGION_ASIA.contains(request.getRegion()) && (sgActive || indActive)) { // SG, AP, IND
                    // Ignore server models that is from other region.
                    if (!REGION_ASIA.contains(model.getServerRegion())) {
                        continue;
                    }

                    // Remove India region from our queuing model.
                    if (!indActive && model.getServerRegion().equals(Region.IND)) {
                        continue;
                    }
                }

                // If the request was from WEST, then check if any servers in EU or US is active
                // Do the same thing if both SG and IND region is inactive.
                if (REGION_WESTERN.contains(request.getRegion()) || (!sgActive && !indActive)) {
                    // Ignore if the model is located in ASIA region for US and EU region.
                    if (REGION_WESTERN.contains(request.getRegion()) && REGION_ASIA.contains(model.getServerRegion())) {
                        continue;
                    }

                    boolean usActive = proxyManager.isRegionActive(Region.US);
                    boolean euActive = proxyManager.isRegionActive(Region.EU);

                    // If any of the region active, then try filtering out any region that is not active.
                    // Otherwise, we will pick any servers in EU or US that is currently available.
                    if (euActive || usActive) {
                        // Check if US region is inactive, then we filter that out.
                        if (!usActive && model.getServerRegion().equals(Region.US)) {
                            continue;
                        }

                        // Same for EU region here.
                        if (!euActive && model.getServerRegion().equals(Region.EU)) {
                            continue;
                        }
                    }
                }

                totalServers++;
                if (model.getPlayerCount() < model.getMaxPlayerCount() || request.isCanJoinFull()) {
                    correctServers.add(model);
                } else {
                    fullServers++;
                }
            }
        }

        if (fullServers == totalServers && totalServers != 0) {
            result.setResultCode(ResultCode.FULL);
            return result;
        }

        // If there were no servers that we have found, retry only if the region queuing mode is enabled.
        // If the region queueing mode is already disabled. There are no available servers for us to join.
        if (correctServers.size() == 0) {
            if (request.isRegionQueueingEnabled() && !fallback) {
                request.setRegionQueueingEnabled(false);

                return matchmake(request, false); // Retry with region queueing disabled, but don't use fallback.
            } else if (!request.isRegionQueueingEnabled() && !fallback) {
                return matchmake(request, true); // Retry with fallback
            } else {
                return result;
            }
        }

        correctServers.sort(ServerModel::compareTo);

        ServerModel foundServer = null;
        ServerModel lowestNonQueueingRegionalServer = null;
        for (ServerModel model : correctServers) {
            // We save the server in the same region as the player with the lowest player count so that it may be used for queueing
            if (request.getCurrentRegion().equals(model.getRegion()) && lowestNonQueueingRegionalServer == null) {
                lowestNonQueueingRegionalServer = model;
            }

            if (request.getQueueingMode().equals(QueueingMode.GLOBAL)) {
                if (model.isQueueingState()) {
                    foundServer = model;
                    break;
                }

            } else if (request.getQueueingMode().equals(QueueingMode.FORCED_TOUCH_ONLY)) {
                if (model.isTouchOnlyState()) {
                    foundServer = model; // in case we find a touch only queueing server
                    break;
                }

            } else if (request.getQueueingMode().equals(QueueingMode.PREFERRED_TOUCH_ONLY)) {
                if (model.isTouchOnlyState()) {
                    foundServer = model;
                    break;
                }

                if (model.isQueueingState()) {
                    foundServer = model;
                }
            }
        }

        if (foundServer == null) {
            if (lowestNonQueueingRegionalServer != null) {
                foundServer = lowestNonQueueingRegionalServer;
            } else {
                foundServer = correctServers.get(0); // if we haven't found any, return the server with the lowest player count(the first in the list).
            }
        }

        if (foundServer != null) {
            registry.getMetricsManager().increaseEffectiveMatchmakerRuns();

            result.setResultCode(ResultCode.FOUND);
            result.setServerUniqueId(foundServer.getServerUniqueId());
        }

        return result;
    }
}
