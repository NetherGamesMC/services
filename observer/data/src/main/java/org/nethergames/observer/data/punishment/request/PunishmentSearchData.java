package org.nethergames.observer.data.punishment.request;

import lombok.*;
import org.nethergames.observer.data.punishment.Punishment;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PunishmentSearchData {
    private long currentOffset;
    private long nextOffset;
    private List<Punishment> punishments;
}
