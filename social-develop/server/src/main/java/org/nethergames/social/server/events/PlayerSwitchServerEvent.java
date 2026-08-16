package org.nethergames.social.server.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlayerSwitchServerEvent {
    private String xuid;
    private String previousServer;
    private String newServer;
}
