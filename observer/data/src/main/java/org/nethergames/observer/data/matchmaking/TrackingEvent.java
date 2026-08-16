package org.nethergames.observer.data.matchmaking;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TrackingEvent {
    private String eventType;
    private String eventData;
    private String timestamp;
}
