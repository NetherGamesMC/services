package org.nethergames.gsms.data;

import lombok.Getter;

/**
 * the predicates implement region compatability rules.
 * As EU and US players will have extended latencies on AP servers, they are not able to queue on these servers.
 * We need to use strings to avoid forward enum references.
 */
public enum Region {
    EU(100, 150),
    US(100, 150),
    IND(100, 150),
    AP(100, 120);

    @Getter
    private final int minDeactivationPlayers;
    @Getter
    private final int startActivationPlayers;

    Region(int minPlayers, int maxPlayers) {
        this.minDeactivationPlayers = minPlayers;
        this.startActivationPlayers = maxPlayers;
    }
}
