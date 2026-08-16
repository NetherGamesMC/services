package org.nethergames.observer.data.punishment.request.punishment;

import lombok.*;
import org.nethergames.observer.data.punishment.Punishment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BulkPunishment {
    public String xuid;
    public List<Punishment> punishments;

    public BulkPunishment(String xuid) {
        this.xuid = xuid;
        this.punishments = new ArrayList<>();
    }
}
