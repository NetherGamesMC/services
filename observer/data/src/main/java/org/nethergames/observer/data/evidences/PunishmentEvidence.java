package org.nethergames.observer.data.evidences;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.nethergames.observer.data.evidences.type.EvidenceType;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PunishmentEvidence implements Cloneable {
    private String punishmentId;
    private long evidenceId;
    private String player;
    private String attachedBy;
    private EvidenceType type;
    private String data;
    private String note;

    public int hashCode() {
        int result = attachedBy.hashCode();
        result = 31 * result + punishmentId.hashCode();
        result = 31 * result + data.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + Long.hashCode(evidenceId);
        if (note != null) {
            result = 31 * result + note.hashCode();
        }

        return result;
    }

    @Override
    public PunishmentEvidence clone() {
        try {
            return (PunishmentEvidence) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
