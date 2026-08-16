package org.nethergames.common.domain.model;

import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public interface ISpecification<T extends IEntity<?>> {

	List<CriteriaDefinition> toCriteria();

	Query toQuery();
}
