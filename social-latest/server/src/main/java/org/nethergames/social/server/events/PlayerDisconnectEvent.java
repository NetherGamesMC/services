package org.nethergames.social.server.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PlayerDisconnectEvent {
    private String xuid;
    private String playerName;
}
