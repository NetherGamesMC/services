package org.nethergames.social.data.request.party.web;

import org.nethergames.social.data.request.party.PartyData.MemberStatus;

import lombok.Data;

@Data
public class UpdatePlayerMemberStatusBody {
    private String sourceXuid;
    private String targetXuid;
    private MemberStatus status;
}
