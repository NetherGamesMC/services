package org.nethergames.observer.data.reports;

import lombok.*;
import org.nethergames.observer.data.reports.request.PlayerReportData;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServerReportBroadcast {
    private PlayerReportEntry reportEntry;
    private PlayerReportData reportData;
}
