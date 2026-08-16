package org.nethergames.observer.data.punishment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.nethergames.observer.data.evidences.PunishmentEvidence;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Punishment implements Comparable<Punishment> {

    public static final String DATABASE_IDENTIFIER = "_id";

    private String id;
    private String xuid;
    private String issuedBy;
    private String note;
    private PunishmentReason reason;
    private boolean permanent;
    private long issuedAt;
    private long validUntil;

    @BsonIgnore
    private List<PunishmentEvidence> evidences = new ArrayList<>();

    public PunishmentEvidence getEvidence(long id) {
        return evidences.stream().filter(i -> i.getEvidenceId() == id).findFirst().orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Punishment that = (Punishment) o;

        if (permanent != that.permanent) return false;
        if (issuedAt != that.issuedAt) return false;
        if (validUntil != that.validUntil) return false;
        if (!xuid.equals(that.xuid)) return false;
        if (!issuedBy.equals(that.issuedBy)) return false;
        return reason.equals(that.reason);
    }

    @Override
    public int hashCode() {
        int result = xuid.hashCode();
        result = 31 * result + issuedBy.hashCode();
        result = 31 * result + (note != null ? note.hashCode() : 0);
        result = 31 * result + reason.hashCode();
        result = 31 * result + (permanent ? 1 : 0);
        result = 31 * result + (int) (issuedAt ^ (issuedAt >>> 32));
        result = 31 * result + (int) (validUntil ^ (validUntil >>> 32));
        return result;
    }

    @Override
    public int compareTo(Punishment punishment) {
        return Long.compare(getValidUntil(), punishment.getValidUntil());
    }
}
