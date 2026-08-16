package org.nethergames.observer.data.tracing.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TracingWhitelistEntry {
    private String originXuid;
    private String exclusionXuid;

    @Override
    public int hashCode() {
        return 31 * originXuid.hashCode() + exclusionXuid.hashCode();
    }
}
