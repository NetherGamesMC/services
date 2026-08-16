package org.nethergames.social.data.request.locality;

import lombok.Data;

import java.util.Map;

@Data
public class LocalitySignupRequest {
    private String sourceId;
    private Map<String, String> currentData;
}
