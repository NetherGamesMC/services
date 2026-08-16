package org.nethergames.observer.data.reports.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerReportData {
    private String player;
    private String reporter;
    private String reportReason;
    private String serverLocation;
    private String replayId;
}
