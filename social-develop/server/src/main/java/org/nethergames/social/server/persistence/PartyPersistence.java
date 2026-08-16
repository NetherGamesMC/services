package org.nethergames.social.server.persistence;

import lombok.extern.log4j.Log4j2;
import org.nethergames.social.data.request.party.Party;
import org.nethergames.social.server.persistence.impl.NoopPartyImpl;
import org.nethergames.social.server.persistence.impl.RedisPartyImpl;

import java.util.Locale;
import java.util.Set;

@Log4j2(topic="PartyPersistence")
public abstract class PartyPersistence {
    /**
     * This will do an upsert of the party into the database
     */
    public abstract void persistParty(Party party);

    public abstract void deleteParty(String partyId);

    public abstract Party findParty(String partyId);

    public abstract Set<Party> loadParties();

    public static PartyPersistence autodetectPartyStorage() {
        String provider = System.getenv("PARTY_PERSISTANCE_PROVIDER");

        if (provider == null) {
            log.warn("No persistence provider set, fallbacking to NOOP Provider");
            return new NoopPartyImpl();
        }

        switch (provider.toUpperCase(Locale.ROOT)) {
            case "REDIS":
                return new RedisPartyImpl();
            case "NOOP":
                return new NoopPartyImpl();
        }


        log.warn("Invalid persistence provider set: {}, fallbacking to NOOP Provider", provider);
        return new NoopPartyImpl();
    }
}
