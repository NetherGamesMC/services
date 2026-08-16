package org.nethergames.observer.data.general;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UsernamePunishmentEntry {
    private String xuid;
    private String currentUsername;
    private String currentPunishment;
}
