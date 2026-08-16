package org.nethergames.gsms.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.nethergames.common.domain.model.BaseEntity;
import org.nethergames.gsms.domain.constants.ScalingAction;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(ScalingDecision.COLL_NAME)
public class ScalingDecision extends BaseEntity<String> {

	public static final @Transient String COLL_NAME = "gs_scaling_evaluator";

	@NotBlank
	private String deployment;

	@NotNull
	private ScalingAction action;

	@NotNull
	private Instant validUntil;

	@NotNull
	private int delta;

	@NotNull
	private Boolean active;

	@NotNull
	private Boolean executed;

	public String getDeployment() {
		return deployment;
	}

	public void setDeployment(String deployment) {
		this.deployment = deployment;
	}

	public ScalingAction getAction() {
		return action;
	}

	public void setAction(ScalingAction action) {
		this.action = action;
	}

	public int getDelta() {
		return delta;
	}

	public void setDelta(int delta) {
		this.delta = delta;
	}

	public Instant getValidUntil() {
		return validUntil;
	}

	public void setValidUntil(Instant validUntil) {
		this.validUntil = validUntil;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean getExecuted() {
		return executed;
	}

	public void setExecuted(Boolean executed) {
		this.executed = executed;
	}
}

