package org.nethergames.gsms.data.model;

public class ProxyStatusBody {
    private String proxyId;
    private int playerCount;

    public ProxyStatusBody(int playerCount) {
        this.playerCount = playerCount;
    }

    public ProxyStatusBody() {
    }

    public ProxyStatusBody(String proxyId, int playerCount) {
        this.proxyId = proxyId;
        this.playerCount = playerCount;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public String getProxyId() {
        return proxyId;
    }

    public void setProxyId(String proxyId) {
        this.proxyId = proxyId;
    }
}
