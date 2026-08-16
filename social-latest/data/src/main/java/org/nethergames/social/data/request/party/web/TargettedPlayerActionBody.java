package org.nethergames.social.data.request.party.web;

import lombok.Data;

@Data
public class TargettedPlayerActionBody {
    private String sourceXuid;
    private String targetXuid;
}
