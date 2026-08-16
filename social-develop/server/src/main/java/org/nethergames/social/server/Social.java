package org.nethergames.social.server;

import com.google.common.eventbus.EventBus;
import io.sentry.Sentry;
import lombok.Getter;
import org.nethergames.social.server.impl.SocialService;
import org.nethergames.social.server.manager.LocalityManager;
import org.nethergames.social.server.manager.PartyManager;
import org.nethergames.social.server.manager.SourceManager;
import org.nethergames.social.server.persistence.PartyPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class Social {
    private static Social instance;

    private final Logger logger = LoggerFactory.getLogger("Service");
    private final SourceManager sourceManager = new SourceManager();
    private final LocalityManager localityManager = new LocalityManager();
    private final PartyManager partyManager = new PartyManager(PartyPersistence.autodetectPartyStorage(), localityManager);
    private final EventBus eventBus = new EventBus();
    private final SocialService socialService;

    public Social() {
        instance = this;

        logger.info("Starting SocialService.");

        if (System.getenv("SENTRY_DSN") != null) {
            Sentry.init(System.getenv("SENTRY_DSN"));
        }

        socialService = new SocialService(7500);

        eventBus.register(localityManager);
    }

    public static Social getInstance() {
        return instance;
    }
}
