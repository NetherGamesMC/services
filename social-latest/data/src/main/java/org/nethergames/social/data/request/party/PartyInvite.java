package org.nethergames.social.data.request.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartyInvite {
    private String xuid;
    private String partyId;

    public org.nethergames.social.rpc.PartyInvite toGRPC() {
        return org.nethergames.social.rpc.PartyInvite.newBuilder()
                .setPartyId(partyId)
                .setXuid(xuid)
                .build();
    }
}
