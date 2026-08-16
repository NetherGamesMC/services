package org.nethergames.gsms.domain.model.filter;

import io.swagger.v3.oas.annotations.Parameter;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerRegion;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.common.domain.model.BaseFilter;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

@ParameterObject
public class GameServerStateFilter extends BaseFilter {

	@Parameter(description = "The pod uuid")
	private List<String> ids;

	@Parameter(description = "Partial search field filter. To filter by server region, server type, game type, or other relevant fields.")
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

	@Parameter(description = "The player count less than equals filter")
	private Integer playerCountLte;

	@Parameter(description = "The player count greater than equals filter")
	private Integer playerCountGte;

	@Parameter(description = "The max player count less than equals filter")
	private Integer maxPlayerCountLte;

	@Parameter(description = "The max player count greater than equals filter")
	private Integer maxPlayerCountGte;

	@Parameter(description = "The queueing state filter")
	private Boolean queueingState;

	@Parameter(description = "The touch only state filter")
	private Boolean touchOnlyState;

	@Parameter(description = "The last tps less than equals filter")
	private Float lastTpsLte;

	@Parameter(description = "The last tps greater than equals filter")
	private Float lastTpsGte;

	@Parameter(description = "The last usage less than equals filter")
	private Float lastUsageLte;

	@Parameter(description = "The last usage greater than equals filter")
	private Float lastUsageGte;

	@Parameter(description = "The last memory usage less than equals filter")
	private Float lastMemoryUsageLte;

	@Parameter(description = "The last memory usage greater than equals filter")
	private Float lastMemoryUsageGte;

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
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

	public Integer getPlayerCountLte() {
		return playerCountLte;
	}

	public void setPlayerCountLte(Integer playerCountLte) {
		this.playerCountLte = playerCountLte;
	}

	public Integer getPlayerCountGte() {
		return playerCountGte;
	}

	public void setPlayerCountGte(Integer playerCountGte) {
		this.playerCountGte = playerCountGte;
	}

	public Integer getMaxPlayerCountLte() {
		return maxPlayerCountLte;
	}

	public void setMaxPlayerCountLte(Integer maxPlayerCountLte) {
		this.maxPlayerCountLte = maxPlayerCountLte;
	}

	public Integer getMaxPlayerCountGte() {
		return maxPlayerCountGte;
	}

	public void setMaxPlayerCountGte(Integer maxPlayerCountGte) {
		this.maxPlayerCountGte = maxPlayerCountGte;
	}

	public Boolean getQueueingState() {
		return queueingState;
	}

	public void setQueueingState(Boolean queueingState) {
		this.queueingState = queueingState;
	}

	public Boolean getTouchOnlyState() {
		return touchOnlyState;
	}

	public void setTouchOnlyState(Boolean touchOnlyState) {
		this.touchOnlyState = touchOnlyState;
	}

	public Float getLastTpsLte() {
		return lastTpsLte;
	}

	public void setLastTpsLte(Float lastTpsLte) {
		this.lastTpsLte = lastTpsLte;
	}

	public Float getLastTpsGte() {
		return lastTpsGte;
	}

	public void setLastTpsGte(Float lastTpsGte) {
		this.lastTpsGte = lastTpsGte;
	}

	public Float getLastUsageLte() {
		return lastUsageLte;
	}

	public void setLastUsageLte(Float lastUsageLte) {
		this.lastUsageLte = lastUsageLte;
	}

	public Float getLastUsageGte() {
		return lastUsageGte;
	}

	public void setLastUsageGte(Float lastUsageGte) {
		this.lastUsageGte = lastUsageGte;
	}

	public Float getLastMemoryUsageLte() {
		return lastMemoryUsageLte;
	}

	public void setLastMemoryUsageLte(Float lastMemoryUsageLte) {
		this.lastMemoryUsageLte = lastMemoryUsageLte;
	}

	public Float getLastMemoryUsageGte() {
		return lastMemoryUsageGte;
	}

	public void setLastMemoryUsageGte(Float lastMemoryUsageGte) {
		this.lastMemoryUsageGte = lastMemoryUsageGte;
	}
}
