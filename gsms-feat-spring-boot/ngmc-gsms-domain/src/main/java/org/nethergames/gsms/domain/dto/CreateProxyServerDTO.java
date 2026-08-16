package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.dto.BaseCreationDTO;
import org.nethergames.common.domain.model.IEntityDTO;
import org.nethergames.gsms.domain.constants.ServerStatus;

import java.util.UUID;

@Schema(description = "DTO for creating a proxy server")
public class CreateProxyServerDTO extends BaseCreationDTO implements IEntityDTO<UUID> {

	@NotBlank(message = "Id must not be blank")
	@Size(max = 24, message = "Id must be between 1 to 24 characters")
	@Schema(description = "Id", requiredMode = Schema.RequiredMode.REQUIRED)
	private UUID id;

	@Schema(description = "The proxy metadata name")
	@NotBlank(message = "The proxy metadata name")
	private String proxyId;

	@NotNull(message = "Region cannot be null")
	private ServerRegion region;

	@Positive(message = "Player count must be positive")
	private int playerCount;

	@Override
	public boolean isNew() {
		return null == this.getId();
	}

	@Override
	public UUID getId() {
		return this.id;
	}

	@Override
	public void setId(UUID id) {
		this.id = id;
	}

	public String getProxyId() {
		return proxyId;
	}

	public void setProxyId(String proxyId) {
		this.proxyId = proxyId;
	}

	public ServerRegion getRegion() {
		return region;
	}

	public void setRegion(ServerRegion region) {
		this.region = region;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public void setPlayerCount(int playerCount) {
		this.playerCount = playerCount;
	}

	@Override
	public Long getVersion() {
		return null;
	}

	@Override
	public void setVersion(Long version) {
	}
}
