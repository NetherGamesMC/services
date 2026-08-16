package org.nethergames.observer.data.punishment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PointMappingCalculationResult {
    private PointMapping pointMapping;
    private String breakdown;
}
