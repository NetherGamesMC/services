package org.nethergames.observer.data.punishment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerComment {
    private String xuid;
    private String publishedBy;
    private long publishedAt;
    private String comment;
}
