package org.nethergames.gsms.infra.listener;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.nethergames.common.domain.constants.GameType;
import org.nethergames.common.domain.constants.ServerType;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.infra.service.ClientEventCommandService;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class ClusterChangedMessageListener implements MessageListener<ChangeStreamDocument<Document>, GameServerState> {

	private final ClientEventCommandService clientEventCommandService;

	public ClusterChangedMessageListener(ClientEventCommandService clientEventCommandService) {
		this.clientEventCommandService = clientEventCommandService;
	}

	@Override
	public void onMessage(@NonNull Message<ChangeStreamDocument<Document>, GameServerState> message) {
		GameServerState state = message.getBody() == null ? message.getBodyBeforeChange() : message.getBody();
		if (state != null && state.getMetadata() != null) {
			GameType gameType = state.getMetadata().getGameType();
			ServerType serverType = state.getMetadata().getServerType();

			if (gameType != null && serverType != null) {
				clientEventCommandService.broadcastClusterEvent(gameType, serverType);
			}
		}

		// TODO: Broadcast lobby selection
	}
}
