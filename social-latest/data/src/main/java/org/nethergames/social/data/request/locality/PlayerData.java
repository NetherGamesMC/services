package org.nethergames.social.data.request.locality;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PlayerData {
    private String playerName;
    private String playerXuid;
    private String serverId;
    private String sourceId;
    private String address;
}
