package org.nethergames.observer.server.generator;

import java.util.UUID;

public class RandomIdGenerator {

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
