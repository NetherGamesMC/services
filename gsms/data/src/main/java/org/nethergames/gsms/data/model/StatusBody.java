package org.nethergames.gsms.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.nethergames.gsms.data.model.ServerModel.Status;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusBody {
    private String serverUniqueId;
    private int playerCount;
    private int maxPlayerCount;
    private boolean queueingState;
    private boolean touchOnlyState;
    private Status status = Status.RUNNING;
    private float tps = 0;
    private float usage = 0;
    private float memoryUsage = 0;
    private String proxyServerInfoType;
    private boolean usingGrpc = false;
}
