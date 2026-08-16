package org.nethergames.common.domain.model;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;

public interface IEntity<ID extends Serializable> extends Persistable<ID> {

	void setId(ID id);

	ID getId();
}
