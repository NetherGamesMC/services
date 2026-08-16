package org.nethergames.gsms.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.model.BaseEntity;
import org.nethergames.common.domain.model.IVersionable;
import org.nethergames.gsms.domain.constants.ServerStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(ProxyServer.COLL_NAME)
public class ProxyServer extends BaseEntity<UUID> implements IVersionable {

	public static final @Transient String COLL_NAME = "gs_proxy_servers";

	@NotBlank(message = "Proxy ID cannot be blank")
	private String proxyId;

	@NotNull(message = "Region cannot be null")
	private ServerRegion region;

	@Positive(message = "Player count must be positive")
	private int playerCount;

	@Version
	private Long version;

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
		return version;
	}

	@Override
	public void setVersion(Long version) {
		this.version = version;
	}
}
