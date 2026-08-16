package org.nethergames.observer.data.tracing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AltTracingDataset {

    private String xuid;
    private Set<String> ip = new HashSet<>();
    private Set<String> deviceId = new HashSet<>();
    private Set<String> selfSignedId = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AltTracingDataset that = (AltTracingDataset) o;

        return Objects.equals(xuid, that.xuid);
    }

    @Override
    public int hashCode() {
        return xuid != null ? xuid.hashCode() : 0;
    }
}
