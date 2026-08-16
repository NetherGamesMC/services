package org.nethergames.observer.data.tracing.request;

import lombok.*;
import org.nethergames.observer.data.tracing.type.TracingType;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class TracingSearchResult {
    private int depth;
    private Map<TracingType, Set<String>> tracingMatches;
}
