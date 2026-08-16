package org.nethergames.gsms.domain.mapper;

import com.google.common.collect.Sets;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.nethergames.common.domain.util.ServerIdUtil;
import org.nethergames.gsms.domain.constants.ServerMode;
import org.nethergames.gsms.domain.constants.ServerStatus;
import org.nethergames.gsms.domain.dto.CreateGameServerDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;
import org.nethergames.gsms.domain.model.values.NetworkEndpoint;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface KubernetesPodMapper {

	default CreateGameServerDTO toCreateGameServerDTO(Pod pod) {
		CreateGameServerDTO dto = new CreateGameServerDTO();
		dto.setId(UUID.fromString(pod.getMetadata().getUid()));
		dto.setServerId(ServerIdUtil.fromString(pod.getMetadata().getName()).toString());
		dto.setMode(ServerMode.UNKNOWN);
		dto.setConnected(false);

		if (StringUtils.isNotBlank(pod.getStatus().getPodIP())) {
			dto.setStatus(ServerStatus.RUNNING);

			NetworkEndpoint endpoint = new NetworkEndpoint();
			endpoint.setIp(pod.getStatus().getPodIP());
			dto.setEndpoint(endpoint);
			dto.setProtocols(Sets.newHashSet());
		} else {
			dto.setStatus(ServerStatus.WAITING);
		}

		return dto;
	}

	default UpdateGameServerDTO toUpdateGameServerDTO(GameServerListDTO server, NetworkEndpoint endpoint) {
		UpdateGameServerDTO dto = new UpdateGameServerDTO();
		dto.setId(server.getId());
		dto.setServerId(server.getServerId());
		dto.setMode(server.getMode());
		dto.setEndpoint(endpoint);
		dto.setProtocols(server.getProtocols());
		dto.setStatus(server.getStatus());
		dto.setVersion(server.getVersion());
		dto.setConnected(server.getConnected());
		return dto;
	}

}
