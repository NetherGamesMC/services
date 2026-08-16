package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.dto.BaseEntityDTO;
import org.nethergames.gsms.domain.constants.ServerStatus;

import java.util.UUID;

@Schema(description = "Proxy server default domain transfer object")
public class ProxyServerListDTO extends BaseEntityDTO<UUID> {

	@Schema(description = "The proxy metadata name")
	private String proxyId;

	@Schema(description = "Server status")
	private ServerStatus status;

	@NotNull(message = "Region cannot be null")
	private ServerRegion region;

	@Positive(message = "Player count must be positive")
	private int playerCount;

	@Override
	public UUID getId() {
		return id;
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

	public ServerStatus getStatus() {
		return status;
	}

	public void setStatus(ServerStatus status) {
		this.status = status;
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
}
