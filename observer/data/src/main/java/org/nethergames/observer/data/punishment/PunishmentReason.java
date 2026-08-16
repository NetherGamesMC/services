package org.nethergames.observer.data.punishment;

import lombok.*;
import org.nethergames.observer.data.punishment.type.PunishmentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PunishmentReason {

    private String name;
    private PunishmentType type;
    private boolean hasRollback = false;
    private boolean adminOnly = false;
    private boolean internalOnly = false;
    private int points;
    private String category;
}
