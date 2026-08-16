package org.nethergames.common.mongodb;

import org.jspecify.annotations.NullMarked;
import org.nethergames.common.mongodb.repo.impl.ExtendedMongoRepositoryImpl;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;

public class ExtendedMongoRepositoryFactory extends MongoRepositoryFactory {

	public ExtendedMongoRepositoryFactory(MongoOperations mongoOperations) {
		super(mongoOperations);
	}

	@Override
	@NullMarked
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return ExtendedMongoRepositoryImpl.class;
	}
}