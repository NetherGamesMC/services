package org.nethergames.gsms.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.nethergames.gsms.data.QueueingMode;
import org.nethergames.gsms.data.Region;

import java.util.Locale;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchmakingRequest {
    private String serverType;
    private String gameType;
    private String currentRegion;
    private boolean canJoinFull;
    private boolean regionQueueingEnabled;
    private QueueingMode queueingMode;

    public Region getRegion() {
        return Region.valueOf(getCurrentRegion().toUpperCase(Locale.ROOT));
    }
}
