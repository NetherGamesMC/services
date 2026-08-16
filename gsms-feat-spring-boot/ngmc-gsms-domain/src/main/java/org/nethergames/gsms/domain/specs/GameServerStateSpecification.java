package org.nethergames.gsms.domain.specs;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.nethergames.common.domain.model.ISpecification;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameServerStateSpecification implements ISpecification<GameServerState> {

	private final GameServerStateFilter filter;

	public GameServerStateSpecification(GameServerStateFilter filter) {
		this.filter = filter;
	}

	@Override
	public List<CriteriaDefinition> toCriteria() {
		List<CriteriaDefinition> criterias = new ArrayList<>();

		if (!CollectionUtils.isEmpty(filter.getIds())) {
			criterias.add(Criteria.where("_id").in(filter.getIds().stream().map(ObjectId::new).collect(Collectors.toSet())));
		}

		if (StringUtils.isNotBlank(filter.getQueries())) {
			String query = Pattern.quote(filter.getQueries());
			criterias.add(new Criteria().orOperator(
					Criteria.where("metadata.serverRegion").regex(query, "i"),
					Criteria.where("metadata.serverType").regex(query, "i"),
					Criteria.where("metadata.gameType").regex(query, "i"),
					Criteria.where("metadata.deploymentId").regex(query, "i"),
					Criteria.where("metadata.replicaId").regex(query, "i"))
			);
		}

		if (CollectionUtils.isNotEmpty(filter.getServerRegions())) {
			criterias.add(Criteria.where("metadata.serverRegion").in(filter.getServerRegions()));
		}

		if (CollectionUtils.isNotEmpty(filter.getServerTypes())) {
			criterias.add(Criteria.where("metadata.serverType").in(filter.getServerTypes()));
		}

		if (CollectionUtils.isNotEmpty(filter.getGameTypes())) {
			criterias.add(Criteria.where("metadata.gameType").in(filter.getGameTypes()));
		}

		if (StringUtils.isNotBlank(filter.getDeploymentId())) {
			criterias.add(Criteria.where("metadata.deploymentId").in(filter.getDeploymentId()));
		}

		if (StringUtils.isNotBlank(filter.getReplicaId())) {
			criterias.add(Criteria.where("metadata.replicaId").in(filter.getReplicaId()));
		}

		if (filter.getPlayerCountLte() != null) {
			criterias.add(Criteria.where("playerCount").lte(filter.getPlayerCountLte()));
		}

		if (filter.getPlayerCountGte() != null) {
			criterias.add(Criteria.where("playerCount").gte(filter.getPlayerCountGte()));
		}

		if (filter.getMaxPlayerCountLte() != null) {
			criterias.add(Criteria.where("maxPlayerCount").lte(filter.getMaxPlayerCountLte()));
		}

		if (filter.getMaxPlayerCountGte() != null) {
			criterias.add(Criteria.where("maxPlayerCount").gte(filter.getMaxPlayerCountGte()));
		}

		if (filter.getQueueingState() != null) {
			criterias.add(Criteria.where("queueingState").is(filter.getQueueingState()));
		}

		if (filter.getTouchOnlyState() != null) {
			criterias.add(Criteria.where("touchOnlyState").is(filter.getTouchOnlyState()));
		}

		if (filter.getLastTpsLte() != null) {
			criterias.add(Criteria.where("lastTps").lte(filter.getLastTpsLte()));
		}

		if (filter.getLastTpsGte() != null) {
			criterias.add(Criteria.where("lastTps").gte(filter.getLastTpsGte()));
		}

		if (filter.getLastUsageLte() != null) {
			criterias.add(Criteria.where("lastUsage").lte(filter.getLastUsageLte()));
		}

		if (filter.getLastUsageGte() != null) {
			criterias.add(Criteria.where("lastUsage").gte(filter.getLastUsageGte()));
		}

		if (filter.getLastMemoryUsageLte() != null) {
			criterias.add(Criteria.where("lastMemoryUsage").lte(filter.getLastMemoryUsageLte()));
		}

		if (filter.getLastMemoryUsageGte() != null) {
			criterias.add(Criteria.where("lastMemoryUsage").gte(filter.getLastMemoryUsageGte()));
		}

		return criterias;
	}

	@Override
	public Query toQuery() {
		Query query = new Query();
		List<CriteriaDefinition> criterias = toCriteria();
		if (null != criterias) {
			for (CriteriaDefinition criteria : criterias) {
				query.addCriteria(criteria);
			}
		}

		return query;
	}
}
