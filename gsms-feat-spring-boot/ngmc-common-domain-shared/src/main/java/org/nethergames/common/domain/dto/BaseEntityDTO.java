package org.nethergames.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import org.nethergames.common.domain.model.IEntityDTO;
import org.nethergames.common.domain.model.IVersionable;

import java.io.Serializable;

@JsonIgnoreProperties({"new", "isNew"})
public class BaseEntityDTO<ID extends Serializable> implements IEntityDTO<ID>, IVersionable {

	@NotNull
	protected ID id;

	protected Long version;

	public BaseEntityDTO() {}

	public BaseEntityDTO(ID id) {
		this.id = id;
	}

	public BaseEntityDTO(ID id, Long version) {
		this(id);
		this.version = version;
	}

	@Override
	public ID getId() {
		return id;
	}

	@Override
	public void setId(ID id) {
		this.id = id;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	@Override
	public void setVersion(Long version) {
		this.version = version;
	}

	@Override
	public boolean isNew() {
		return null == this.getId();
	}

}
