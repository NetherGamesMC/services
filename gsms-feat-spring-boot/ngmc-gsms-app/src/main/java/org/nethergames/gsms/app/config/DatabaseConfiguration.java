package org.nethergames.gsms.app.config;

import com.mongodb.client.model.changestream.FullDocumentBeforeChange;
import io.mongock.runner.springboot.EnableMongock;
import org.jspecify.annotations.Nullable;
import org.nethergames.common.mongodb.ExtendedMongoRepositoryFactoryBean;
import org.nethergames.common.mongodb.repo.impl.ExtendedMongoRepositoryImpl;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.infra.listener.ClusterChangedMessageListener;
import org.nethergames.gsms.infra.listener.ServerChangedMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.event.ValidatingEntityCallback;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;

@Configuration
@EnableMongock
@Import(value = MongoAutoConfiguration.class)
@EnableMongoRepositories(basePackages = {
		"org.nethergames.gsms.domain.repository",
		"org.nethergames.common.mongodb.repo",
},
		repositoryFactoryBeanClass = ExtendedMongoRepositoryFactoryBean.class,
		repositoryBaseClass = ExtendedMongoRepositoryImpl.class
)
public class DatabaseConfiguration implements RuntimeHintsRegistrar {

	private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

	@Bean
	@Primary
	public MongoTemplate mongoTemplate(MongoDatabaseFactory dbFactory, MongoMappingContext context, MongoCustomConversions customConversions) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(dbFactory);
		MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, context);
		converter.setCustomConversions(customConversions);
		converter.afterPropertiesSet();
		return new MongoTemplate(dbFactory, converter);
	}

	@Bean
	public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
		return new MongoTransactionManager(dbFactory);
	}

	@Bean
	public ValidatingEntityCallback validatingMongoEventListener(LocalValidatorFactoryBean localValidator) {
		return new ValidatingEntityCallback(localValidator);
	}

	@Bean
	public LocalValidatorFactoryBean localValidator() {
		return new LocalValidatorFactoryBean();
	}

	@Bean
	public MongoCustomConversions customConversions() {
		return new MongoCustomConversions(new ArrayList<>(Jsr310Converters.getConvertersToRegister()));
	}

	@Bean
	public MessageListenerContainer messageListenerContainer(
			MongoTemplate template,
			ClusterChangedMessageListener clusterMessageListener,
			ServerChangedMessageListener serverMessageListener) {

		MessageListenerContainer container = new DefaultMessageListenerContainer(template);
		container.start();

		// Server-based event listener
		container.register(ChangeStreamRequest.builder(clusterMessageListener)
				.fullDocumentBeforeChangeLookup(FullDocumentBeforeChange.REQUIRED)
				.collection(GameServerState.COLL_NAME)
				.build(), GameServerState.class);

		// Proxy-based event listener
		container.register(ChangeStreamRequest.builder(serverMessageListener)
				.fullDocumentBeforeChangeLookup(FullDocumentBeforeChange.REQUIRED)
				.collection(GameServer.COLL_NAME)
				.build(), GameServer.class);

		return container;
	}

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		log.info("Registering mongo hints");

		hints.reflection().registerType(ExtendedMongoRepositoryImpl.class, MemberCategory.values());
	}
}
