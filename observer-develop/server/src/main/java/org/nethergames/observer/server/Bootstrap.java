package org.nethergames.observer.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger("Bootstrap");

    public static void main(String[] args) {
        try {
            new Observer();
        } catch (Throwable t) {
            logger.error("Error in Observer Server", t);
        }
    }
}
