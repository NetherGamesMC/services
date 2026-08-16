package org.nethergames.gsms.domain.mapper;

import org.mapstruct.Mapper;
import org.nethergames.gsms.domain.constants.ServerStatus;

@Mapper(componentModel = "spring")
public interface ServerStatusMapper {

	default ServerStatus toServerStatus(org.nethergames.gsms.rpc.ServerStatus status) {
		return switch (status) {
			case STATUS_ONLINE -> ServerStatus.RUNNING;
			case STATUS_TERMINATING -> ServerStatus.TERMINATING;
			case UNRECOGNIZED -> ServerStatus.WAITING;
		};
	}
}
