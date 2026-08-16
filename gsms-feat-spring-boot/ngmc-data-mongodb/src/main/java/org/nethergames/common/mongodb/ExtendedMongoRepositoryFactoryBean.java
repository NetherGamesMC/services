package org.nethergames.common.mongodb;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactoryBean;
import org.springframework.data.repository.Repository;

import java.io.Serializable;

public class ExtendedMongoRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends MongoRepositoryFactoryBean<T, S, ID> {

	public ExtendedMongoRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	@NullMarked
	protected MongoRepositoryFactory getFactoryInstance(MongoOperations operations) {
		return new ExtendedMongoRepositoryFactory(operations);
	}
}