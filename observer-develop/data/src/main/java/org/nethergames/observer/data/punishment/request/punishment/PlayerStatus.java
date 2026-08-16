package org.nethergames.observer.data.punishment.request.punishment;

import lombok.*;
import org.nethergames.observer.data.punishment.PlayerComment;
import org.nethergames.observer.data.punishment.PointMapping;
import org.nethergames.observer.data.punishment.Punishment;

import java.util.Collection;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PlayerStatus {
    private Punishment ban;
    private Punishment mute;
    private Map<String, PointMapping> points;
    private Collection<PlayerComment> comments;
}
