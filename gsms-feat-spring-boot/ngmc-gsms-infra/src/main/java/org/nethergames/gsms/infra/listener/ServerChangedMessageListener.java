package org.nethergames.gsms.infra.listener;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.infra.evaluator.ServerChangeEvaluator;
import org.nethergames.gsms.domain.mapper.GameServerMapper;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.infra.service.ClientEventCommandService;
import org.nethergames.gsms.rpc.ServerEventType;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ServerChangedMessageListener implements MessageListener<ChangeStreamDocument<Document>, GameServer> {

	private final GameServerMapper mapper;

	private final ServerChangeEvaluator changeEvaluator;

	private final ClientEventCommandService clientEventCommandService;

	public ServerChangedMessageListener(
			GameServerMapper mapper,
			ServerChangeEvaluator changeEvaluator,
			ClientEventCommandService clientEventCommandService) {

		this.mapper = mapper;
		this.changeEvaluator = changeEvaluator;
		this.clientEventCommandService = clientEventCommandService;
	}

	@Override
	public void onMessage(Message<ChangeStreamDocument<Document>, GameServer> message) {
		GameServer current = message.getBody();
		GameServer previous = message.getBodyBeforeChange();

		ChangeStreamDocument<Document> rawMessage = message.getRaw();
		if (rawMessage == null) {
			return;
		}

		Optional<ServerEventType> eval = changeEvaluator.determineEventType(rawMessage.getOperationType(), previous, current);
		eval.ifPresent(eventType -> {
			GameServerListDTO dto = mapper.toListViewDTO(current != null ? current : previous);
			clientEventCommandService.broadcastProxyEvent(dto, eventType);
		});
	}
}
