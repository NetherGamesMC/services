package org.nethergames.gsms.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.nethergames.gsms.data.Region;

import java.util.Locale;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerModel implements Comparable<ServerModel> {
    private String region;
    private String serverUniqueId;
    private Status status = Status.RUNNING;
    private String serverType = "";
    private String gameType = "";
    private String address = "";
    private String deploymentName = "";
    private int port;
    private int playerCount;
    private int maxPlayerCount;
    private boolean queueingState;
    private boolean touchOnlyState;
    private float lastTps;
    private float lastUsage;
    private float lastMemoryUsage;
    private String proxyServerInfoType;

    private int timeout = 0;
    private long lastSuccessfulPush = 0;

    @Override
    public int compareTo(ServerModel model) {
        return Integer.compare(getPlayerCount(), model.getPlayerCount());
    }

    @Override
    public boolean equals(Object model) {
        return model instanceof ServerModel srvModel && srvModel.getServerUniqueId().equalsIgnoreCase(getServerUniqueId());
    }

    public boolean compareChanges(StatusBody status) {
        return this.playerCount != status.getPlayerCount() ||
                this.maxPlayerCount != status.getMaxPlayerCount() ||
                this.queueingState != status.isQueueingState() ||
                this.touchOnlyState != status.isTouchOnlyState() ||
                this.status != status.getStatus();
    }

    public ServerModel applyUpdate(StatusBody status, String ip, int port) {
        this.playerCount = status.getPlayerCount();
        this.maxPlayerCount = status.getMaxPlayerCount();
        this.queueingState = status.isQueueingState();
        this.touchOnlyState = status.isTouchOnlyState();
        this.status = status.getStatus();
        this.lastTps = status.getTps();
        this.lastUsage = status.getUsage();
        this.lastMemoryUsage = status.getMemoryUsage();
        this.timeout = Math.max(--this.timeout, 0);
        this.address = ip;
        this.port = port;
        this.proxyServerInfoType = status.getProxyServerInfoType();

        return this;
    }

    public Region getServerRegion() {
        return Region.valueOf(getRegion().toUpperCase(Locale.ROOT));
    }

    public boolean isTimeout() {
        return timeout++ > 10;
    }

    public enum Status {
        RUNNING,
        TERMINATING
    }
}
