package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.nethergames.common.domain.model.IVersionable;

@Schema(description = "DTO for updating a proxy server")
public class UpdateProxyServerDTO extends CreateProxyServerDTO implements IVersionable {

	@NotNull(message = "Mongodb document version number must not be null")
	@Schema(description = "Mongodb document version number", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long version;

	@Override
	public Long getVersion() {
		return this.version;
	}

	@Override
	public void setVersion(Long version) {
		this.version = version;
	}
}
