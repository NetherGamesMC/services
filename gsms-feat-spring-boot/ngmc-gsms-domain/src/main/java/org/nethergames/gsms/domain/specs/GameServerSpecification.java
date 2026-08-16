package org.nethergames.gsms.domain.specs;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.common.domain.model.ISpecification;
import org.nethergames.gsms.domain.model.filter.GameServerFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GameServerSpecification implements ISpecification<GameServer> {

	private final GameServerFilter filter;

	public GameServerSpecification(GameServerFilter filter) {
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

		if (CollectionUtils.isNotEmpty(filter.getServerIds())) {
			criterias.add(Criteria.where("serverId").in(filter.getServerIds()));
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

		if (CollectionUtils.isNotEmpty(filter.getModes())) {
			criterias.add(Criteria.where("mode").in(filter.getModes(), "i"));
		}

		if (CollectionUtils.isNotEmpty(filter.getStatus())) {
			criterias.add(Criteria.where("status").in(filter.getStatus(), "i"));
		}

		if (filter.getCreatedAtLte() != null) {
			criterias.add(Criteria.where("createdAt").lte(filter.getCreatedAtLte()));
		}

		if (filter.getCreatedAtGte() != null) {
			criterias.add(Criteria.where("createdAt").gte(filter.getCreatedAtGte()));
		}

		if (filter.getConnected() != null) {
			criterias.add(Criteria.where("connected").is(filter.getConnected()));
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
