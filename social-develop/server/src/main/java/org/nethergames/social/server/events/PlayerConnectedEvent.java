package org.nethergames.social.server.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerConnectedEvent {
    private String xuid;
    private String playerName;
}
