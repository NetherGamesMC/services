package org.nethergames.observer.data.matchmaking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MatchParticipation {
    private int participationId;
    private String matchId;
    private String xuid;
    private boolean winner;
    private String team;
    private int kills;
    private int deaths;
}
