package org.nethergames.observer.data.matchmaking.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class StartGameRequest {
    private String serverType;
    private String gameType;
    private String map;
}
