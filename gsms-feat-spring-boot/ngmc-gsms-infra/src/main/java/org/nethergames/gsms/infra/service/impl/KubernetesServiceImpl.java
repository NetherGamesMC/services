package org.nethergames.gsms.infra.service.impl;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.nethergames.gsms.domain.constants.ServerStatus;
import org.nethergames.gsms.domain.dto.CreateGameServerDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;
import org.nethergames.gsms.domain.mapper.KubernetesPodMapper;
import org.nethergames.gsms.domain.model.values.NetworkEndpoint;
import org.nethergames.gsms.infra.service.GameServerCommandService;
import org.nethergames.gsms.infra.service.GameServerQueryService;
import org.nethergames.gsms.infra.service.GameServerStateCommandService;
import org.nethergames.gsms.infra.service.KubernetesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KubernetesServiceImpl implements KubernetesService {

	private static final Logger log = LoggerFactory.getLogger(KubernetesServiceImpl.class);

	private final GameServerQueryService queryService;
	private final GameServerCommandService commandService;
	private final GameServerStateCommandService stateCommandService;
	private final KubernetesClient client;
	private final KubernetesPodMapper mapper;

	@Value("${gsms.kubernetes.infra-namespace:infra-dev}")
	private String kubernetesNamespace;

	public KubernetesServiceImpl(
			GameServerQueryService queryService,
			GameServerCommandService commandService,
			GameServerStateCommandService stateCommandService,
			KubernetesClient client,
			KubernetesPodMapper mapper) {

		this.queryService = queryService;
		this.commandService = commandService;
		this.stateCommandService = stateCommandService;
		this.client = client;
		this.mapper = mapper;
	}

	@Override
	public Pod getProxyFromName(String podName) {
		PodList proxyPod = client.pods().inNamespace(kubernetesNamespace)
				.withField("metadata.name", podName)
				.list();

		if (proxyPod != null && !CollectionUtils.isEmpty(proxyPod.getItems())) {
			return proxyPod.getItems().getFirst();
		}

		return null;
	}

	@Override
	@Transactional
	public void handlePodCreateInformer(Pod pod) {
		List<GameServerListDTO> optServer = queryService.findAll(List.of(pod.getMetadata().getUid()));

		if (CollectionUtils.isEmpty(optServer)) {
			CreateGameServerDTO dto = mapper.toCreateGameServerDTO(pod);
			commandService.addAll(List.of(dto));

			log.info("[{}][{}] pod registered into database in waiting state.", pod.getMetadata().getUid(), dto.getServerId());
		} else {
			handlePodUpdateInformer(pod, null);
		}
	}

	@Override
	@Transactional
	public void handlePodUpdateInformer(Pod oldPod, Pod newPod) {
		// Determine if this pod needs any changes or not.
		if (newPod != null) {
			var oldPodResourceVersion = oldPod.getMetadata().getResourceVersion();
			var newPodResourceVersion = newPod.getMetadata().getResourceVersion();

			if (oldPodResourceVersion.equals(newPodResourceVersion)) {
				return;
			}
		}

		// When updating, the entity we want to update MUST always exist, if it does not, we can't update it.
		List<GameServerListDTO> optServer = queryService.findAll(List.of(oldPod.getMetadata().getUid()));
		GameServerListDTO server = optServer.stream().findFirst().orElse(null);

		if (server != null) {
			NetworkEndpoint endpoint;
			if (server.getEndpoint() == null) {
				endpoint = new NetworkEndpoint();
				endpoint.setIp(oldPod.getStatus().getPodIP());
			} else {
				endpoint = server.getEndpoint();
			}

			// Updates endpoint when pod transitions or IP changes.
			if (handlePodTransitions(oldPod, newPod, server, endpoint)) {
				UpdateGameServerDTO dto = mapper.toUpdateGameServerDTO(server, endpoint);
				commandService.updateAll(List.of(dto));
			}
		}
	}

	@Override
	@Transactional
	public void handlePodDeleteInformer(Pod pod) {
		commandService.deleteAll(List.of(pod.getMetadata().getUid()));
		stateCommandService.deleteAll(List.of(pod.getMetadata().getUid()));

		log.info("[{}][{}] removed from the server list.", pod.getMetadata().getUid(), pod.getMetadata().getName());
	}

	private boolean handlePodTransitions(Pod oldPod, Pod newPod, GameServerListDTO server, NetworkEndpoint endpoint) {
		if (newPod == null) {
			return true;
		}

		// Flags
		boolean isMarkedForDeletion = newPod.isMarkedForDeletion() && !oldPod.isMarkedForDeletion();
		boolean isIpAddressChanged = !Strings.CS.equals(oldPod.getStatus().getPodIP(), newPod.getStatus().getPodIP());

		// Pod is marked for deletion
		if (isMarkedForDeletion) {
			server.setStatus(ServerStatus.TERMINATING);

			log.info("[{}][{}] is now in terminating state.", server.getId(), server.getServerId());
		}

		// Pod IP address changed
		if (isIpAddressChanged) {
			endpoint.setIp(newPod.getStatus().getPodIP());

			// Logging that is based on the previous GSMS implementation.
			if (oldPod.getStatus().getPodIP() == null) {
				log.info("[{}][{}] has assigned the IP of {}", server.getId(), server.getServerId(), endpoint.getIp());
			} else {
				log.info("[{}][{}] has replaced the IP from {} to {}", server.getId(), server.getServerId(), oldPod.getStatus().getPodIP(), endpoint.getIp());
			}
		}

		return isMarkedForDeletion || isIpAddressChanged;
	}
}
