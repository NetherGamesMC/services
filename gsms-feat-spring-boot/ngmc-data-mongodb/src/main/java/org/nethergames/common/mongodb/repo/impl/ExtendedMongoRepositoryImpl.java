package org.nethergames.common.mongodb.repo.impl;

import com.mongodb.client.result.DeleteResult;
import org.nethergames.common.domain.model.ISpecification;
import org.nethergames.common.mongodb.repo.ExtendedMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@NoRepositoryBean
public class ExtendedMongoRepositoryImpl<T, ID> extends SimpleMongoRepository<T, ID> implements ExtendedMongoRepository<T, ID> {

	private final MongoEntityInformation<T, ID> metadata;
	private final MongoOperations mongoOperations;

	/**
	 * Creates a new {@link SimpleMongoRepository} for the given {@link MongoEntityInformation} and {@link MongoTemplate}.
	 *
	 * @param metadata        must not be {@literal null}.
	 * @param mongoOperations must not be {@literal null}.
	 */
	public ExtendedMongoRepositoryImpl(MongoEntityInformation<T, ID> metadata, MongoOperations mongoOperations) {
		super(metadata, mongoOperations);

		this.metadata = metadata;
		this.mongoOperations = mongoOperations;
	}

	@Override
	public long count(Query query) {
		query.allowSecondaryReads();
		return mongoOperations.count(query, metadata.getCollectionName());
	}

	@Override
	public long deleteByIds(Iterable<ID> ids) {
		Query query = getIdsQuery(ids);
		DeleteResult result = mongoOperations.remove(query, metadata.getJavaType(), metadata.getCollectionName());
		return result.getDeletedCount();
	}

	@Override
	public long deleteAll(Query query) {
		DeleteResult result = mongoOperations.remove(query, metadata.getJavaType(), metadata.getCollectionName());
		return result.getDeletedCount();
	}

	@Override
	public Page<T> findAll(ISpecification<?> spec, Pageable pageable) {
		return findAll(spec.toQuery(), pageable);
	}

	@Override
	public Page<T> findAll(Query query, Pageable pageable) {
		Assert.notNull(pageable, "Pageable must not be null");

		long count = count(query);
		List<T> list = mongoOperations.find(query.with(pageable).allowSecondaryReads(), metadata.getJavaType(), metadata.getCollectionName());
		return PageableExecutionUtils.getPage(list, pageable, () -> count);
	}

	private Query getIdsQuery(Iterable<ID> ids) {
		return new Query(getIdsCriteria(ids));
	}

	private Criteria getIdsCriteria(Iterable<ID> ids) {
		return where(metadata.getIdAttribute()).in(Streamable.of(ids).stream().collect(StreamUtils.toUnmodifiableList()));
	}
}
