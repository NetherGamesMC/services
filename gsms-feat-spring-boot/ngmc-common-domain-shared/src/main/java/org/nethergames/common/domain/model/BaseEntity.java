package org.nethergames.common.domain.model;

import org.springframework.data.annotation.Id;

import java.io.Serializable;

public abstract class BaseEntity<ID extends Serializable> implements IEntity<ID> {
	@Id
	private ID id;

	@Override
	public ID getId() {
		return id;
	}

	@Override
	public void setId(ID id) {
		this.id = id;
	}

	@Override
	public boolean isNew() {
		return null == this.getId();
	}
}
