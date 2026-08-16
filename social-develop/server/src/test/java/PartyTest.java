import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.nethergames.social.data.request.exception.MissingPermissionException;
import org.nethergames.social.data.request.exception.NotInPartyException;
import org.nethergames.social.data.request.locality.LocalityEntry;
import org.nethergames.social.data.request.party.Party;
import org.nethergames.social.data.request.party.PartyData;
import org.nethergames.social.data.request.party.PartyInvite;
import org.nethergames.social.server.manager.LocalityManager;
import org.nethergames.social.server.manager.PartyManager;
import org.nethergames.social.server.persistence.PartyPersistence;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class PartyTest {

    @Mock
    private LocalityManager localityService;

    private PartyManager partyManager;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        partyManager = new PartyManager(PartyPersistence.autodetectPartyStorage(), localityService);
    }

    @Test
    void CreateAndDisbandParty() {
        var localityEntry = new LocalityEntry();
        localityEntry.setPlayerName("dummy");

        when(localityService.getPlayerByXuid("TobiasDev")).thenReturn(localityEntry);

        partyManager.createParty("TobiasDev");
        partyManager.disbandParty("TobiasDev");
    }

    @Test
    void DisbandUnknownParty() {
        assertThrows(NotInPartyException.class, () -> partyManager.disbandParty("ImNotKnown"));
    }

    /**
     * This test does the following:
     * - Create a New Party
     * - Invite a new member
     * - Test whether the invite works correctly
     * - Test whether the invite is removed correctly after accepting invites
     * - Test whether the player is added to the party correctly
     * - Test whether the party permissions are working correctly
     * - Test whether the leaving mechanism works
     */
    @Test
    void PartyInviteValidation() {
        var localityEntry = new LocalityEntry();
        localityEntry.setPlayerName("dummy");

        when(localityService.getPlayerByXuid("Test1")).thenReturn(localityEntry);
        when(localityService.getPlayerByXuid("Test2")).thenReturn(localityEntry);
        when(localityService.getPlayerByXuid("Test3")).thenReturn(localityEntry);

        partyManager.createParty("Test1");
        partyManager.invitePlayer("Test1", "Test2");
        List<PartyInvite> invites = partyManager.getPendingPlayerPartyInvites("Test2");

        assertEquals(1, invites.size());

        partyManager.acceptInvite("Test2", invites.get(0).getPartyId());

        invites = partyManager.getPendingPlayerPartyInvites("Test2");

        assertEquals(0, invites.size());

        Party playerParty = partyManager.getPlayerParty("Test2");

        assertNotNull(playerParty); // The party should not be null because Test2 is now a member of the party

        assertEquals(2, playerParty.getMembers().size());

        assertThrows(MissingPermissionException.class, () -> partyManager.disbandParty("Test2"));
        assertThrows(MissingPermissionException.class, () -> partyManager.invitePlayer("Test2", "Test3"));
        // This should throw since the player is only a party member and does not have any other permissions

        partyManager.leaveParty("Test2");

        playerParty = partyManager.getPlayerParty("Test2");

        assertNull(playerParty);

        Party party = partyManager.getPlayerParty("Test1");
        assertEquals(1, party.getMembers().size());
        partyManager.disbandParty("Test1");

        party = partyManager.getPlayerParty("Test1");
        assertNull(party);
    }


    @Test
    void PermissionTest() {
        var localityEntry = new LocalityEntry();
        localityEntry.setPlayerName("dummy");

        when(localityService.getPlayerByXuid("Player1")).thenReturn(localityEntry);
        when(localityService.getPlayerByXuid("Player2")).thenReturn(localityEntry);
        when(localityService.getPlayerByXuid("Player3")).thenReturn(localityEntry);
        when(localityService.getPlayerByXuid("Player4")).thenReturn(localityEntry);

        Party party = partyManager.createParty("Player1"); // Leader
        partyManager.invitePlayer("Player1", "Player2"); // Moderator
        partyManager.invitePlayer("Player1", "Player3"); // Member
        partyManager.acceptInvite("Player2", party.getId());
        partyManager.acceptInvite("Player3", party.getId());

        partyManager.updateMemberStatus("Player1", "Player2", PartyData.MemberStatus.MODERATOR);

        // Member tries to kick
        assertThrows(MissingPermissionException.class, () -> partyManager.kickFromParty("Player3", "Player2"));
        // Member tries to promote to Leader
        assertThrows(MissingPermissionException.class, () -> partyManager.updateMemberStatus("Player3", "Player2", PartyData.MemberStatus.LEADER));
        // Member tries to promote to Moderator
        assertThrows(MissingPermissionException.class, () -> partyManager.updateMemberStatus("Player3", "Player2", PartyData.MemberStatus.MODERATOR));
        // Member tries to invite
        assertThrows(MissingPermissionException.class, () -> partyManager.invitePlayer("Player3", "Player4"));
        // Member tries to disband
        assertThrows(MissingPermissionException.class, () -> partyManager.disbandParty("Player3"));

        // Moderator disbands party
        assertThrows(MissingPermissionException.class, () -> partyManager.disbandParty("Player2"));
        // Moderator changes leader status to member
        assertThrows(MissingPermissionException.class, () -> partyManager.updateMemberStatus("Player2", "Player1", PartyData.MemberStatus.MEMBER));
        // Moderator changes member status to Moderator
        assertThrows(MissingPermissionException.class, () -> partyManager.updateMemberStatus("Player2", "Player3", PartyData.MemberStatus.MODERATOR));

        // All actions that they can do

        // Moderator invites player
        partyManager.invitePlayer("Player2", "Player4");
        partyManager.acceptInvite("Player4", party.getId());

        // Moderator kicks player
        partyManager.kickFromParty("Player2", "Player4");

        // Leader demotes moderator
        partyManager.updateMemberStatus("Player1", "Player2", PartyData.MemberStatus.MEMBER);

        // Leader kicks member
        partyManager.kickFromParty("Player1", "Player2");

        // Leader promotes to moderator
        partyManager.updateMemberStatus("Player1", "Player3", PartyData.MemberStatus.MODERATOR);

        // Leader gives Leadership to Moderator
        partyManager.updateMemberStatus("Player1", "Player3", PartyData.MemberStatus.LEADER);

        // new leader is actually leader
        assertEquals(party.getPlayerStatus("Player3"), PartyData.MemberStatus.LEADER);

        // Old member is actually moderator
        assertEquals(party.getPlayerStatus("Player1"), PartyData.MemberStatus.MODERATOR);

    }
}
