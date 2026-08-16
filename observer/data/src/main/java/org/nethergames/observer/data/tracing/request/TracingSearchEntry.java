package org.nethergames.observer.data.tracing.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.nethergames.observer.data.tracing.type.TracingType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TracingSearchEntry {

    private int depth = -1;
    private Set<String> xuidList = new HashSet<>();
    private Set<String> exclusions = new HashSet<>();
    private EnumSet<TracingType> searchConditions = EnumSet.allOf(TracingType.class);

    public TracingSearchEntry(int depth, Set<String> xuidList) {
        this.depth = depth;
        this.xuidList = xuidList;
    }

    public TracingSearchEntry(int depth, EnumSet<TracingType> searchConditions) {
        this.depth = depth;
        this.searchConditions = searchConditions;
    }

    public TracingSearchEntry(int depth, Set<String> xuidList, EnumSet<TracingType> searchConditions) {
        this.depth = depth;
        this.xuidList = xuidList;
        this.searchConditions = searchConditions;
    }
}
