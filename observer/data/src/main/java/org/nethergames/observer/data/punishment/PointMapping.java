package org.nethergames.observer.data.punishment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.joda.time.DateTime;

import java.time.Instant;


@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PointMapping {
    public static final float DECREASE_TIME = 60 * 60 * 24 * 30; // 1 Month = 1 Point decreased

    private int points;
    private long lastInfraction;
    private long infractionUntil;

    @JsonIgnore
    private Punishment lastPunishment = null;

    public void addPoints(int points) {
        this.points = Math.min(Math.max(this.points + points, 0), 16);
    }

    public boolean isPunishmentActive() {
        return points == 16 || infractionUntil > Instant.now().getEpochSecond();
    }

    public DateTime calculatePunishmentTime() {
        DateTime time = DateTime.now();

        if (points < 4) {
            time = time.plusDays(1);
        } else if (points < 6) {
            time = time.plusWeeks(1);
        } else if (points < 8) {
            time = time.plusWeeks(2);
        } else if (points < 10) {
            time = time.plusMonths(1);
        } else if (points < 12) {
            time = time.plusMonths(2);
        } else if (points < 14) {
            time = time.plusMonths(4);
        } else if (points < 16) {
            time = time.plusMonths(8);
        } else {
            return null;
        }

        return time;
    }
}