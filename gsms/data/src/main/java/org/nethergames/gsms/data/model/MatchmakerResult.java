package org.nethergames.gsms.data.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchmakerResult {
    private ResultCode resultCode;
    private String serverUniqueId;
    private String responseId;
    private String traceId;

    public enum ResultCode {
        FOUND,
        NONE_FOUND,
        FULL
    }
}
