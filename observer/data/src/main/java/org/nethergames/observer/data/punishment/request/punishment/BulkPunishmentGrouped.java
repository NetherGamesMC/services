package org.nethergames.observer.data.punishment.request.punishment;

import lombok.*;
import org.nethergames.observer.data.punishment.Punishment;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BulkPunishmentGrouped {
    public String xuid;
    public Map<String, List<Punishment>> punishments;
}
