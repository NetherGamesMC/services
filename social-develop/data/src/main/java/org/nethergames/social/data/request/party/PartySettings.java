package org.nethergames.social.data.request.party;

import lombok.Data;

@Data
public class PartySettings {
    private boolean publicParty = false;
    private boolean privateMatch = false;
    private boolean playerRandomisation = false;
    private int maxSize = 30;

    public org.nethergames.social.rpc.PartySettings toGRPC() {
        return org.nethergames.social.rpc.PartySettings.newBuilder()
                .setPublicParty(publicParty)
                .setPrivateGames(privateMatch)
                .setMaxSize(maxSize)
                .build();
    }
}
