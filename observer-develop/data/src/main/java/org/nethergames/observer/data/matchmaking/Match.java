package org.nethergames.observer.data.matchmaking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Match {

    private String id;

    private String serverType;
    private String gameType;
    private String map;
    private long startedAt;
    private long endedAt;
    private List<TrackingEvent> events;
}
