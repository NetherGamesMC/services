package org.nethergames.observer.data.punishment.request;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PunishmentCreationData {
    private String xuid;
    private String name;
    private String issuer;
    private String reason;
    private String note;
}
