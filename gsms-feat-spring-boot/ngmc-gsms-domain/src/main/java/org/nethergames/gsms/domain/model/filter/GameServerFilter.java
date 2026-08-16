package org.nethergames.gsms.domain.model.filter;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Pattern;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.gsms.domain.constants.*;
import org.nethergames.common.domain.model.BaseFilter;
import org.springdoc.core.annotations.ParameterObject;

import java.time.Instant;
import java.util.List;

@ParameterObject
public class GameServerFilter extends BaseFilter {

	@Parameter(description = "The pod uuid")
	private List<String> ids;

	@Parameter(description = "Server pod unique identifier")
	private List<String> serverIds;

	@Parameter(description = "Partial search field filter. To filter by server name, IP address, or other relevant fields.")
	private String queries;

	@Parameter(description = "The region of the server")
	private List<ServerRegion> serverRegions;

	@Parameter(description = "The type of the server")
	private List<ServerType> serverTypes;

	@Parameter(description = "The game type of the server")
	private List<GameType> gameTypes;

	@Parameter(description = "The pod deployment id")
	private String deploymentId;

	@Parameter(description = "The pod replica id")
	private String replicaId;

	@Parameter(description = "The connection status of the server")
	private Boolean connected;

	@Parameter(description = "The game server mode")
	private List<ServerMode> modes;

	@Parameter(description = "The status of the game server")
	private List<ServerStatus> status;

	@Parameter(description = "Created time less than or equal to filter")
	private Instant createdAtLte;

	@Parameter(description = "Created time greater than or equal to filter")
	private Instant createdAtGte;

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}

	public List<String> getServerIds() {
		return serverIds;
	}

	public void setServerIds(List<String> serverIds) {
		this.serverIds = serverIds;
	}

	public String getQueries() {
		return queries;
	}

	public void setQueries(String queries) {
		this.queries = queries;
	}

	public List<ServerRegion> getServerRegions() {
		return serverRegions;
	}

	public void setServerRegions(List<ServerRegion> serverRegions) {
		this.serverRegions = serverRegions;
	}

	public List<ServerType> getServerTypes() {
		return serverTypes;
	}

	public void setServerTypes(List<ServerType> serverTypes) {
		this.serverTypes = serverTypes;
	}

	public List<GameType> getGameTypes() {
		return gameTypes;
	}

	public void setGameTypes(List<GameType> gameTypes) {
		this.gameTypes = gameTypes;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getReplicaId() {
		return replicaId;
	}

	public void setReplicaId(String replicaId) {
		this.replicaId = replicaId;
	}

	public Boolean getConnected() {
		return connected;
	}

	public void setConnected(Boolean connected) {
		this.connected = connected;
	}

	public List<ServerMode> getModes() {
		return modes;
	}

	public void setModes(List<ServerMode> modes) {
		this.modes = modes;
	}

	public List<ServerStatus> getStatus() {
		return status;
	}

	public void setStatus(List<ServerStatus> status) {
		this.status = status;
	}

	public Instant getCreatedAtLte() {
		return createdAtLte;
	}

	public void setCreatedAtLte(Instant createdAtLte) {
		this.createdAtLte = createdAtLte;
	}

	public Instant getCreatedAtGte() {
		return createdAtGte;
	}

	public void setCreatedAtGte(Instant createdAtGte) {
		this.createdAtGte = createdAtGte;
	}
}
