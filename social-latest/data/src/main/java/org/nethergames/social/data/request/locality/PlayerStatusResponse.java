package org.nethergames.social.data.request.locality;

import lombok.Data;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@Setter
@ToString
public class PlayerStatusResponse {
    private Map<String, PlayerStatusEntry> data = new HashMap<>();

    @Data
    @ToString
    public static class PlayerStatusEntry {
        private boolean online;
        private PlayerData playerData;
    }
}
