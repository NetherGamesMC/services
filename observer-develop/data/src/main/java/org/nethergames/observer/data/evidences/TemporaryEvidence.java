package org.nethergames.observer.data.evidences;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemporaryEvidence {
    private boolean accepted;
    private String punishmentId;
    private String issuerId;
}
