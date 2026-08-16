package org.nethergames.observer.data.punishment;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PunishmentReasonGrouped {
    private Map<String, List<PunishmentReason>> reasons;
}
