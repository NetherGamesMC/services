package org.nethergames.gsms.server.scheduler;

import io.sentry.Sentry;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.data.Region;
import org.nethergames.gsms.server.manager.ProxyManager;
import org.nethergames.gsms.server.server.ServerRegistry;

import java.util.Map;

@Log4j2(topic = "RegionActivationDeterminator")
public class RegionActivationDeterminator implements Runnable {

    private final ProxyManager proxyManager;

    public RegionActivationDeterminator(ProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public void run() {
        try {
            int currentGlobalPlayers = 0;
            Map<String, Integer> regionNumbers = proxyManager.getRegionNumbers();
            for (Region region : Region.values()) {
                int currentTotalPlayers = regionNumbers.get(region.name());

                boolean activeRegion = proxyManager.isRegionActive(region);
                if (activeRegion && currentTotalPlayers < region.getMinDeactivationPlayers()) {
                    log.info("Region {} is no longer enabled, players will be rerouted. ({}/{})", region.name(), currentTotalPlayers, region.getMinDeactivationPlayers());
                    proxyManager.setRegionActivation(region, false);
                } else if (!activeRegion && currentTotalPlayers > region.getStartActivationPlayers()) {
                    log.info("Region {} meets activation criteria, re-enabling queuing.. ({}/{})", region.name(), currentTotalPlayers, region.getStartActivationPlayers());
                    proxyManager.setRegionActivation(region, true);
                }

                currentGlobalPlayers += currentTotalPlayers;

                proxyManager.getLastRegionCounts().replace(region.name(), currentTotalPlayers);
            }

            proxyManager.setCurrentGlobalCount(currentGlobalPlayers);
        } catch (Throwable t) {
            log.throwing(t);

            Sentry.captureException(t);
        }
    }
}
