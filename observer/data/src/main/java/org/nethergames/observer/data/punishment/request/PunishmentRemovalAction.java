package org.nethergames.observer.data.punishment.request;

import lombok.*;
import org.nethergames.observer.data.punishment.Punishment;

import java.util.List;

@Data
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PunishmentRemovalAction {
    private String issuer;
    private List<Punishment> affectedPunishments;
}
