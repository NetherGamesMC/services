package org.nethergames.gsms.domain.specs;

import org.apache.commons.collections4.CollectionUtils;
import org.bson.types.ObjectId;
import org.nethergames.common.domain.model.ISpecification;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.domain.model.ProxyServer;
import org.nethergames.gsms.domain.model.filter.ProxyServerFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProxyServerSpecification implements ISpecification<ProxyServer> {

	private final ProxyServerFilter filter;

	public ProxyServerSpecification(ProxyServerFilter filter) {
		this.filter = filter;
	}

	@Override
	public List<CriteriaDefinition> toCriteria() {
		List<CriteriaDefinition> criterias = new ArrayList<>();

		if (!CollectionUtils.isEmpty(filter.getIds())) {
			criterias.add(Criteria.where("_id").in(filter.getIds().stream().map(UUID::fromString).collect(Collectors.toSet())));
		}

		return criterias;
	}

	@Override
	public Query toQuery() {
		return null;
	}
}
