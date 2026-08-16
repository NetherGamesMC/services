package org.nethergames.social.data.request.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.nethergames.social.rpc.PartyRole;

import java.util.HashMap;
import java.util.Map;

@Data
public class PartyData {
    private Map<String, Map.Entry<MemberStatus, String>> members = new HashMap<>();
    private PartySettings settings = new PartySettings();

    public boolean isLeader(String xuid) {
        MemberStatus status = members.get(xuid).getKey();

        if (status == null) {
            return false;
        }
        return status == MemberStatus.LEADER;
    }

    public MemberStatus getPlayerStatus(String player) {
        return this.members.get(player).getKey();
    }

    public boolean canKick(String xuid) {
        MemberStatus status = getMembers().get(xuid).getKey();
        return status != null && status.canKick;
    }

    public boolean canDisband(String xuid) {
        MemberStatus status = getMembers().get(xuid).getKey();
        return status != null && status.canDisband;
    }

    public boolean canInvite(String xuid) {
        MemberStatus status = getMembers().get(xuid).getKey();
        return status != null && status.canInvite;
    }

    @AllArgsConstructor
    public enum MemberStatus {

        MEMBER(false, false, false, false, PartyRole.Member),
        // Space for more? :p
        MODERATOR(true, true, false, false, PartyRole.Moderator),

        LEADER(true, true, true, true, PartyRole.Leader);

        public static final MemberStatus[] VALUES = values();
        private final boolean canKick;
        private final boolean canInvite;
        private final boolean canDisband;
        private final boolean canPromote;
        private final PartyRole grpcMapping;

        public boolean canKick() {
            return this.canKick;
        }

        public boolean canInvite() {
            return this.canInvite;
        }

        public boolean canDisband() {
            return this.canDisband;
        }

        public boolean canPromote() {
            return canPromote;
        }

        public PartyRole getGrpcMapping() {
            return grpcMapping;
        }
    }
}
