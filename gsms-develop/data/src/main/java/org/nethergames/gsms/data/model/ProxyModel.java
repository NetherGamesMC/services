package org.nethergames.gsms.data.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nethergames.gsms.data.Region;

import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProxyModel {
    private String proxyId;
    private String region;
    private int playerCount;

    public Region getServerRegion() {
        return Region.valueOf(getRegion().toUpperCase(Locale.ROOT));
    }
}
