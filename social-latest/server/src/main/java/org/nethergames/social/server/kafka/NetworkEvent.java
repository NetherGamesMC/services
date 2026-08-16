package org.nethergames.social.server.kafka;

import lombok.Data;

import java.util.List;

@Data
public class NetworkEvent<T> {
    private final String eventId;
    private final List<String> recipients;
    private final T data;
}
