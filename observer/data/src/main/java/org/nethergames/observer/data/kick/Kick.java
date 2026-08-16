package org.nethergames.observer.data.kick;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kick {
    private String xuid;
    private String reason;
    private String issuedBy;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Kick that = (Kick) o;

        if (!xuid.equals(that.xuid)) return false;
        if (!reason.equals(that.reason)) return false;
        return issuedBy.equals(that.issuedBy);
    }

    public int hashCode() {
        int result = xuid.hashCode();
        result = 31 * result + issuedBy.hashCode();
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Kick;
    }
}
