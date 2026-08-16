package org.nethergames.observer.data.tracing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.bson.codecs.pojo.annotations.BsonIgnore;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class AltTracingPushData {
    @BsonIgnore
    private String username;
    private String xuid;
    private String ip;
    private String deviceId;
    private String selfSignedId;
    private String uuid;
    private String clientRandomId;
}
