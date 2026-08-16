package org.nethergames.social.server.persistence.impl;

import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.nethergames.social.data.request.party.Party;
import org.nethergames.social.server.persistence.PartyPersistence;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.HashSet;
import java.util.Set;

@Log4j2(topic = "RedisPartyImpl")
public class RedisPartyImpl extends PartyPersistence {

    private static final String REDIS_HOST = System.getenv("PARTY_REDIS_HOST") != null ? System.getenv("PARTY_REDIS_HOST") : "redis-leader.infra-prod.svc.cluster.local";
    private static final int REDIS_PORT = System.getenv("PARTY_REDIS_PORT") != null ? Integer.parseInt(System.getenv("PARTY_REDIS_PORT")) : 6379;
    private static final String REDIS_PREFIX = System.getenv("PARTY_REDIS_PREFIX") != null ? System.getenv("PARTY_REDIS_PREFIX") : "social:party:";

    private final JedisPooled storagePool = new JedisPooled(REDIS_HOST, REDIS_PORT);
    private final Gson gson = new Gson();

    @Override
    public void persistParty(Party party) {
        storagePool.jsonSet(REDIS_PREFIX + party.getId(), gson.toJson(party));
    }

    @Override
    public void deleteParty(String partyId) {
        storagePool.del(REDIS_PREFIX + partyId);
    }

    @Override
    public Party findParty(String id) {
        return storagePool.jsonGet(REDIS_PREFIX + id, Party.class);
    }

    public Set<Party> loadParties() {
        Set<Party> parties = new HashSet<>();
        String keyPattern = REDIS_PREFIX + "*";

        // Set the scan parameters
        ScanParams scanParams = new ScanParams().match(keyPattern);

        String cursor = "0";
        do {
            // Perform the scan
            ScanResult<String> scanResult = storagePool.scan(cursor, scanParams);
            for (String key : scanResult.getResult()) {
                // Get the value for each matching key

                try {
                    Party party = storagePool.jsonGet(key, Party.class);
                    if (party == null) {
                        log.warn("Party disappeared during warmup phase: {}", key);
                        continue;
                    }

                    parties.add(party);
                } catch (Throwable t) {
                    log.warn("Could not load party with key " + key + " from storage", t);
                }

            }

            // Update the cursor to continue scanning
            cursor = scanResult.getCursor();
        } while (!cursor.equals("0"));

        return parties;
    }
}
