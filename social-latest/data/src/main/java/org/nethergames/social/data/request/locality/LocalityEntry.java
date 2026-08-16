package org.nethergames.social.data.request.locality;

import lombok.Data;

import java.util.UUID;

@Data
public class LocalityEntry {
    private String sourceUid;
    private String proxyId;
    private String playerName;
    private String playerIdentifier;
    private String location = null;
    private String lastLocation = null;
    private String address;
    private String connectionId = UUID.randomUUID().toString();
}
