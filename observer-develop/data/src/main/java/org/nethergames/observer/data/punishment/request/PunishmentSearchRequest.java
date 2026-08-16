package org.nethergames.observer.data.punishment.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "set")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PunishmentSearchRequest {
    private String targetXuid;
    private String issuerXuid;
    private Date afterIssued;
    private Date beforeIssued;
    private String category;

    private EvidenceType evidenceType;

    public enum EvidenceType {
        ALL,
        ONLY_SUBMITTED,
        NOT_SUBMITTED,
    }
}
