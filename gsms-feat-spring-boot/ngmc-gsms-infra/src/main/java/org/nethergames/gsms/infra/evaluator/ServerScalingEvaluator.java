package org.nethergames.gsms.infra.evaluator;

import org.nethergames.gsms.domain.model.values.ScalingScope;

public interface ServerScalingEvaluator {
	void markDirty(ScalingScope scope);
}
