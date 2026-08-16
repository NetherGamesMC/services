package org.nethergames.social.server.manager;

import com.google.common.eventbus.Subscribe;
import lombok.extern.log4j.Log4j2;
import org.nethergames.social.data.request.exception.*;
import org.nethergames.social.data.request.locality.LocalityEntry;
import org.nethergames.social.data.request.party.Party;
import org.nethergames.social.data.request.party.PartyData;
import org.nethergames.social.data.request.party.PartyInvite;
import org.nethergames.social.server.events.PlayerDisconnectEvent;
import org.nethergames.social.server.events.PlayerSwitchServerEvent;
import org.nethergames.social.server.persistence.PartyPersistence;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log4j2(topic = "PartyManager")
public class PartyManager {
    private final ConcurrentHashMap<String, Party> parties = new ConcurrentHashMap<>();
    private final Set<PartyInvite> invites = new HashSet<>();
    private final PartyPersistence persistenceManager;
    private final LocalityManager localityManager;

    public PartyManager(PartyPersistence persistenceManager, LocalityManager localityManager) {
        log.info("Starting Party Persistence Manager.");

        this.persistenceManager = persistenceManager;
        this.localityManager = localityManager;
        Set<Party> loaded = persistenceManager.loadParties();

        loaded.forEach(party -> parties.put(party.getId(), party));
        log.info("Loaded {} parties from redis storage.", parties.size());
    }

    public Party getPlayerPartyNonNull(String xuid) {
        Party party = getPlayerParty(xuid);
        if (party == null) {
            throw new NotInPartyException(xuid);
        }

        return party;
    }

    public Party getPlayerParty(String xuid) {
        for (Party party : parties.values()) {
            if (party.getMembers().containsKey(xuid)) return party;
        }

        return null;
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectEvent event) {
        Party party = this.getPlayerParty(event.getXuid());

        if (party != null) {
            PartyData.MemberStatus status = party.getPlayerStatus(event.getXuid());

            switch (status) {
                case LEADER:
                    if (party.getMembers().size() == 1) {
                        this.deleteParty(party.getId());
                    } else {
                        // TODO new player gets party lead
                    }
                    break;
                default:
                    removeFromParty(event.getXuid());
            }
        }
    }

    public void joinPublicParty(String sourceXuid, String targetPartyId) {
        Party party = parties.get(targetPartyId);
        LocalityEntry locality = localityManager.getPlayerByXuid(sourceXuid);
        if (party == null) {
            throw new PartyNotFoundException(targetPartyId);
        }

        if (!party.getSettings().isPublicParty()) throw new PartyNotPublicException(targetPartyId);

        if (party.getMembers().size() >= party.getSettings().getMaxSize()) throw new PartyFullException(targetPartyId);

        String playerName = "";
        if (locality != null) playerName = locality.getPlayerName();

        party.getMembers().put(sourceXuid, Map.entry(PartyData.MemberStatus.MEMBER, playerName));
        this.persistParty(party);
        log.info("{} joined public party {}", sourceXuid, party.getId());
    }

    public List<Party> listPublicParties() {
        return this.parties.values().stream().filter(party -> party.getSettings().isPublicParty()).collect(Collectors.toList());
    }

    @Subscribe
    public void onPlayerSwitch(PlayerSwitchServerEvent event) {
        Party party = this.getPlayerParty(event.getXuid());
    }

    public Party invitePlayer(String xuid, String targetXuid) {
        Party ownParty = getPlayerParty(xuid);
        if (ownParty == null) throw new NotInPartyException(xuid);
        PartyData.MemberStatus status = getPlayerStatus(xuid);

        if (status == null || !status.canInvite()) {
            throw new MissingPermissionException(xuid, "invite");
        }

        Party party = getPlayerParty(targetXuid);

        if (party != null) {
            if (party.getId().equals(ownParty.getId())) {
                throw new AlreadyInPartyException(ownParty.getId(), party.getId());
            } else {
                throw new AlreadyInPartyException(party.getId(), party.getId());
            }
        }

        if ((ownParty.getMembers().size() + 1) >= ownParty.getSettings().getMaxSize()) {
            throw new PartyFullException(ownParty.getId());
        }

        invites.add(new PartyInvite(targetXuid, ownParty.getId()));
        log.info("Created new invite for player {} to party {}", targetXuid, ownParty.getId());

        return ownParty;
    }

    public boolean removeFromParty(String xuid) {
        Party party = getPlayerParty(xuid);
        if (party != null) {
            party.getMembers().remove(xuid);
            this.persistParty(party);
            return true;
        }

        return false;
    }

    private void deleteInvite(String playerXuid, String partyId) {
        invites.removeIf(o -> o.getPartyId().equals(partyId) && o.getXuid().equals(playerXuid));
    }

    public void removeInvite(String sourceXuid, String targetXuid) {
        PartyData.MemberStatus status = getPlayerStatus(sourceXuid);
        Party ownParty = getPlayerParty(sourceXuid);
        if (ownParty == null) throw new NotInPartyException(sourceXuid);

        if (status == null || !status.canInvite()) {
            throw new MissingPermissionException(sourceXuid, "invite");
        }

        invites.removeIf(o -> o.getPartyId().equals(ownParty.getId()) && o.getXuid().equals(targetXuid));
    }

    public void clearPlayerInvites(String xuid) {
        invites.removeIf(o -> o.getXuid().equals(xuid));
    }

    public Party leaveParty(String xuid) {
        Party party = getPlayerParty(xuid);

        if (party.getPlayerStatus(xuid).equals(PartyData.MemberStatus.LEADER)) {
            throw new CannotLeaveLeaderException();
        }

        log.info("Player {} left party {}", xuid, party.getId());
        party.getMembers().remove(xuid);
        this.persistParty(party);

        return party;
    }

    public void clearPartyInvites(String partyId) {
        invites.removeIf(o -> o.getPartyId().equals(partyId));
    }

    public List<PartyInvite> getPendingPlayerPartyInvites(String xuid) {
        return invites.stream().filter(o -> o.getXuid().equals(xuid))
                .collect(Collectors.toList());
    }

    public List<PartyInvite> getPendingPartyInvites(String partyId) {
        return invites.stream().filter(o -> o.getPartyId().equals(partyId))
                .collect(Collectors.toList());
    }

    public Party getParty(String partyId) {
        return parties.get(partyId);
    }

    public boolean hasPartyRole(String xuid, String partyId, PartyData.MemberStatus status) {
        Party playerParty = getPlayerParty(xuid);

        if (playerParty == null) return false;

        if (!playerParty.getId().equals(partyId)) return false;

        return playerParty.getMembers().get(xuid).getKey().ordinal() >= status.ordinal();
    }

    public Party kickFromParty(String sourceXuid, String targetXuid) {
        Party party = getPlayerParty(sourceXuid);

        if (party == null) throw new NotInPartyException(sourceXuid);
        if (party.getPlayerStatus(targetXuid) == null) throw new NotInPartyException(targetXuid);

        PartyData.MemberStatus sourceStatus = party.getMembers().get(sourceXuid).getKey();
        PartyData.MemberStatus targetStatus = party.getMembers().get(targetXuid).getKey();
        if (!sourceStatus.canKick() || sourceStatus.ordinal() < targetStatus.ordinal())
            throw new MissingPermissionException(sourceXuid, "kick_member");


        log.info("Player {} kicked {} from the party {}", sourceXuid, targetXuid, party.getId());
        party.getMembers().remove(targetXuid);
        persistParty(party);

        return party;
    }

    public Party acceptInvite(String sourceId, String partyId) {
        Optional<PartyInvite> invite = invites.stream().filter(o -> o.getXuid().equals(sourceId) && o.getPartyId().equals(partyId)).findFirst();
        LocalityEntry locality = localityManager.getPlayerByXuid(sourceId);

        if (invite.isEmpty()) throw new NotInvitedException(partyId);

        Party party = parties.get(partyId);
        if (party == null) {
            deleteInvite(sourceId, partyId);
            throw new PartyNotFoundException(partyId);
        }

        if (party.getMembers().size() >= party.getSettings().getMaxSize()) throw new PartyFullException(partyId);

        String playerName = "";
        if (locality != null) playerName = locality.getPlayerName();

        party.getMembers().put(sourceId, Map.entry(PartyData.MemberStatus.MEMBER, playerName));
        this.persistParty(party);
        deleteInvite(sourceId, partyId);

        log.info("{} joined party {}", sourceId, partyId);

        return party;
    }

    public void declineInvite(String sourceId, String partyId) {
        Optional<PartyInvite> invite = invites.stream().filter(o -> o.getXuid().equals(sourceId) && o.getPartyId().equals(partyId)).findFirst();

        if (invite.isEmpty()) throw new NotInvitedException(partyId);

        Party party = parties.get(partyId);
        if (party == null) {
            removeInvite(sourceId, partyId);
            throw new PartyNotFoundException(partyId);
        }

        removeInvite(sourceId, partyId);

        log.info("{} declined invite to party {}", sourceId, partyId);
    }

    public void movePartyLeadership(String sourceXuid, String targetXuid) {
        Party party = getPlayerParty(sourceXuid);
        LocalityEntry localitySource = localityManager.getPlayerByXuid(sourceXuid);
        LocalityEntry localityTarget = localityManager.getPlayerByXuid(targetXuid);

        if (party == null) throw new NotInPartyException(sourceXuid);
        if (!party.isLeader(sourceXuid)) throw new MissingPermissionException(sourceXuid, "change_leader");

        if (party.getPlayerStatus(targetXuid) == null) throw new NotInPartyException(targetXuid);

        String playerNameSource = "";
        if (localitySource != null) playerNameSource = localitySource.getPlayerName();

        String playerNameTarget = "";
        if (localityTarget != null) playerNameTarget = localityTarget.getPlayerName();

        party.getMembers().put(sourceXuid, Map.entry(PartyData.MemberStatus.MEMBER, playerNameSource));
        party.getMembers().put(targetXuid, Map.entry(PartyData.MemberStatus.LEADER, playerNameTarget));
        this.persistParty(party);
    }

    public PartyData.MemberStatus getPlayerStatus(String xuid) {
        Party playerParty = getPlayerParty(xuid);

        if (playerParty == null) return null;

        return playerParty.getMembers().get(xuid).getKey();
    }

    public Party createParty(String xuid) {
        Party existingParty = getPlayerParty(xuid);
        LocalityEntry locality = localityManager.getPlayerByXuid(xuid);

        if (existingParty != null) throw new AlreadyInPartyException(existingParty.getId(), "new");

        String playerName = "";
        if (locality != null) playerName = locality.getPlayerName();

        Party party = new Party(UUID.randomUUID().toString());
        party.setMember(xuid, playerName, PartyData.MemberStatus.LEADER);

        this.persistParty(party);

        clearPlayerInvites(xuid);
        log.info("Created new party for player {} with ID {}", xuid, party.getId());

        return party;
    }

    public Party updateMemberStatus(String xuid, String targetXuid, PartyData.MemberStatus status) {
        Party playerParty = getPlayerParty(xuid);
        Party targetPlayerParty = getPlayerParty(targetXuid);

        LocalityEntry localitySource = localityManager.getPlayerByXuid(xuid);
        LocalityEntry localityTarget = localityManager.getPlayerByXuid(targetXuid);

        if (!playerParty.getId().equals(targetPlayerParty.getId())) {
            throw new NotInPartyException(targetXuid);
        }

        PartyData.MemberStatus sourceStatus = playerParty.getPlayerStatus(xuid);
        PartyData.MemberStatus targetStatus = playerParty.getPlayerStatus(targetXuid);

        if (!sourceStatus.canPromote()) {
            throw new MissingPermissionException(xuid, "promote_to_" + status.name().toLowerCase(Locale.ROOT));
        }

        if (sourceStatus.ordinal() <= targetStatus.ordinal()) { // The executing player must have a higher permission level than the target player
            throw new MissingPermissionException(xuid, "promote_to_" + status.name().toLowerCase(Locale.ROOT));
        }

        if (!(sourceStatus.equals(PartyData.MemberStatus.LEADER) || sourceStatus.ordinal() > status.ordinal())) {
            throw new MissingPermissionException(xuid, "promote_to_" + status.name().toLowerCase(Locale.ROOT));
        }

        String playerNameSource = "";
        if (localitySource != null) playerNameSource = localitySource.getPlayerName();

        String playerNameTarget = "";
        if (localityTarget != null) playerNameTarget = localityTarget.getPlayerName();

        if (status.equals(PartyData.MemberStatus.LEADER)) {
            // Promotion to leader
            playerParty.setMember(xuid, playerNameSource, PartyData.MemberStatus.MODERATOR);
            playerParty.setMember(targetXuid, playerNameTarget, PartyData.MemberStatus.LEADER);
            // new leader is set, old leader is demoted to member
        } else {
            playerParty.setMember(targetXuid, playerNameTarget, status);
        }

        log.info("Player {} promoted {} to {} in party {}", xuid, targetXuid, status.name(), playerParty.getId());

        persistParty(playerParty);

        return playerParty;
    }

    public Party disbandParty(String xuid) {
        Party ownParty = getPlayerParty(xuid);

        if (ownParty == null) throw new NotInPartyException(xuid);

        PartyData.MemberStatus status = ownParty.getPlayerStatus(xuid);
        if (status == null || !status.canDisband()) {
            throw new MissingPermissionException(xuid, "disband");
        }

        clearPartyInvites(ownParty.getId());
        this.deleteParty(ownParty.getId());
        log.info("Party {} disbanded by player {}", ownParty.getId(), xuid);

        return ownParty;
    }


    private void persistParty(Party party) {
        this.parties.put(party.getId(), party);
        this.persistenceManager.persistParty(party);
    }

    private void deleteParty(String id) {
        this.parties.remove(id);
        this.persistenceManager.deleteParty(id);
    }


}
