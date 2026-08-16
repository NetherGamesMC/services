package org.nethergames.observer.data.auditing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerMessage {
    private String playerXuid;
    private String serverId;
    private String timestamp;
    private boolean flagged;
    private String context;
    private String content;
}
