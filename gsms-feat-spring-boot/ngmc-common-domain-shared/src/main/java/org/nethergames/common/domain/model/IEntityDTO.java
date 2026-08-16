package org.nethergames.common.domain.model;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;

public interface IEntityDTO<ID extends Serializable> extends Persistable<ID> {
	ID getId();

	void setId(ID id);

	Long getVersion();

	void setVersion(Long version);
}
