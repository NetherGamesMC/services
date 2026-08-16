package org.nethergames.observer.data.tracing;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TracingResult {
    private String xuid;
    private ArrayList<Match> relationalFindingKeys = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TracingResult that = (TracingResult) o;

        return xuid.equals(that.getXuid());
    }

    @Override
    public int hashCode() {
        return xuid != null ? xuid.hashCode() : 0;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Match {
        String property;
        String matchValue;
    }
}
