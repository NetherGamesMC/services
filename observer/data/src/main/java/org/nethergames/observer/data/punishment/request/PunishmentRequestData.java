package org.nethergames.observer.data.punishment.request;

import lombok.*;
import org.nethergames.observer.data.punishment.type.PunishmentType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PunishmentRequestData {
    public String xuid;
    public int tracingDepth;
    public boolean activeOnly;
    public PunishmentType[] punishmentTypes = PunishmentType.values();
}
