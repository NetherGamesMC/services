package org.nethergames.observer.data.reports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.nethergames.observer.data.reports.request.PlayerReportData;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerReportEntry {
    private String player;
    private Date lastReported = Date.from(Instant.now());

    private Set<String> playersReported = new HashSet<>();
    private Set<String> matchesReported = new HashSet<>();
    private Map<String, Integer> reportHit = new HashMap<>();
    private String traineeClaimed;
    private int totalReports = 0;

    public PlayerReportEntry(String player) {
        this.player = player;
    }

    public void addReport(PlayerReportData report) {
        lastReported = Date.from(Instant.now());

        var reportHit = getReportHit().getOrDefault(report.getReportReason(), 0);
        getReportHit().put(report.getReportReason(), ++reportHit);

        if (report.getReplayId() != null) {
            getMatchesReported().add(report.getReplayId());
        }

        totalReports++;
    }

    public static class PlayerReportList extends ArrayList<PlayerReportEntry> {

    }

    public static class PlayerReportMap extends HashMap<String, PlayerReportEntry> {

    }
}
