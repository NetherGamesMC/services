package org.nethergames.gsms.infra.service;

import io.grpc.stub.StreamObserver;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.rpc.*;

import java.util.UUID;

public interface ClientEventCommandService {
	boolean registerGameEventStreams(StreamObserver<GameEvent> event);

	void removeGameEventStreams(UUID podUid);

	boolean registerProxyEventStreams(StreamObserver<ServerEvent> responseObserver);

	void removeProxyEventStreams(UUID podUid);

	void onEventReceived(UpdateModel response);

	void onEventReceived(ProxyModel response);

	void broadcastProxyEvent(GameServerListDTO state, ServerEventType eventType);

	void broadcastClusterEvent(GameType gameType, ServerType serverType);
}
