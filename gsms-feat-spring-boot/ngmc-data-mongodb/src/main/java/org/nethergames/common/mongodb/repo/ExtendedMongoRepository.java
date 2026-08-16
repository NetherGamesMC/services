package org.nethergames.common.mongodb.repo;

import org.nethergames.common.domain.model.ISpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Spring-based mongodb repository extension
 */
@NoRepositoryBean
public interface ExtendedMongoRepository<T, ID> extends MongoRepository<T, ID> {

	long count(Query query);

	long deleteByIds(Iterable<ID> ids);

	long deleteAll(Query query);

	Page<T> findAll(ISpecification<?> spec, Pageable pageable);

	Page<T> findAll(Query query, Pageable pageable);

}
