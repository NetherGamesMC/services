package org.nethergames.gsms.data;

import lombok.Getter;

/**
 * the predicates implement region compatability rules.
 * As EU and US players will have extended latencies on AP servers, they are not able to queue on these servers.
 * We need to use strings to avoid forward enum references.
 */
public enum Region {
    EU(20, 40),
    US(20, 40),
    IND(20, 40),
    AP(20, 40);

    @Getter
    private final int minDeactivationPlayers;
    @Getter
    private final int startActivationPlayers;

    Region(int minPlayers, int maxPlayers) {
        this.minDeactivationPlayers = minPlayers;
        this.startActivationPlayers = maxPlayers;
    }
}
