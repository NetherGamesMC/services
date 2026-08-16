package org.nethergames.social.server.persistence.impl;

import org.nethergames.social.data.request.party.Party;
import org.nethergames.social.server.persistence.PartyPersistence;

import java.util.Collections;
import java.util.Set;

public class NoopPartyImpl extends PartyPersistence {

    @Override
    public void persistParty(Party party) {

    }

    @Override
    public void deleteParty(String partyId) {

    }

    @Override
    public Party findParty(String partyId) {
        return null;
    }

    @Override
    public Set<Party> loadParties() {
        return Collections.emptySet();
    }
}
