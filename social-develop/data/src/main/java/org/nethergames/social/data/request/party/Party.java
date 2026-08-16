package org.nethergames.social.data.request.party;

import lombok.Data;
import org.nethergames.social.rpc.PartyMember;

import java.util.Map;


@Data
public class Party extends PartyData {
    private String id;

    public Party(String id) {
        super();
        this.id = id;
    }

    public void setMember(String xuid, String playerName, MemberStatus status) {
        getMembers().put(xuid, Map.entry(status, playerName));
    }

    public org.nethergames.social.rpc.Party toGRPC() {
        var builder = org.nethergames.social.rpc.Party.newBuilder();

        getMembers().forEach((key, value) -> builder.addMembers(PartyMember.newBuilder()
                        .setXuid(key)
                        .setPlayerName(value.getValue())
                        .setRole(value.getKey().getGrpcMapping())
                        .build()
                )
        );

        builder.setSettings(this.getSettings().toGRPC());

        return builder.build();
    }
}
