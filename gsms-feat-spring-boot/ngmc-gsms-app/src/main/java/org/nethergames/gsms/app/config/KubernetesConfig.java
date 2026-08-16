package org.nethergames.gsms.app.config;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.apache.commons.collections4.CollectionUtils;
import org.nethergames.gsms.app.kubernetes.KubernetesPodInformer;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.model.GameServerState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Configuration
public class KubernetesConfig {

	@Value("${gsms.kubernetes.pod-namespace:game-dev}")
	private String podNamespace;

	private final MongoTemplate template;

	public KubernetesConfig(MongoTemplate template) {
		this.template = template;
	}

	@Bean
	public KubernetesClient kubernetesClient(Config config) {
		return new KubernetesClientBuilder()
				.withConfig(config)
				.withTaskExecutor(Executors.newSingleThreadExecutor())
				.build();
	}

	@Transactional
	@Bean(destroyMethod = "close")
	public SharedIndexInformer<Pod> podInformer(KubernetesClient client, KubernetesPodInformer informer) {
		NonNamespaceOperation<Pod, PodList, PodResource> inNamespace = client.pods().inNamespace(podNamespace);
		PodList podList = inNamespace.list();

		// Removes stale GameServer and GameServerState entries, some pods may get deleted after restart.
		if (podList != null && CollectionUtils.isNotEmpty(podList.getItems())) {
			Set<UUID> podIds = podList.getItems().stream()
					.map(Pod::getMetadata)
					.map(ObjectMeta::getUid)
					.map(UUID::fromString)
					.collect(Collectors.toSet());

			template.remove(Query.query(Criteria.where("_id").nin(podIds)), GameServer.class, GameServer.COLL_NAME);
			template.remove(Query.query(Criteria.where("_id").nin(podIds)), GameServerState.class, GameServerState.COLL_NAME);
		}

		return client.pods().inNamespace(podNamespace).inform(informer, 5 * 1000L);
	}
}
