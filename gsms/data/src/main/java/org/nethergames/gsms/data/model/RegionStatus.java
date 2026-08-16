package org.nethergames.gsms.data.model;

public class RegionStatus {
    private int players;
    private boolean enabled;

    public RegionStatus(int players, boolean enabled) {
        this.players = players;
        this.enabled = enabled;
    }

    public RegionStatus() {
    }

    public int getPlayers() {
        return players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
