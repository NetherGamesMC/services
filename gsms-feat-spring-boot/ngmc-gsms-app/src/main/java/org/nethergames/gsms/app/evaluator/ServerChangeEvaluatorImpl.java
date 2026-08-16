package org.nethergames.gsms.app.evaluator;

import com.mongodb.client.model.changestream.OperationType;
import org.apache.commons.lang3.StringUtils;
import org.nethergames.gsms.infra.evaluator.ServerChangeEvaluator;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.rpc.ServerEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ServerChangeEvaluatorImpl implements ServerChangeEvaluator {

	private static final Logger log = LoggerFactory.getLogger(ServerChangeEvaluatorImpl.class);

	@Override
	public Optional<ServerEventType> determineEventType(
			OperationType operation,
			GameServer previous,
			GameServer current
	) {
		if (operation == null) {
			return Optional.empty();
		}

		return switch (operation) {
			case INSERT -> evaluateInsertEvent(current);
			case UPDATE, REPLACE -> evaluateUpdateEvent(previous, current);
			case DELETE -> evaluateDeleteEvent(previous);
			default -> Optional.empty();
		};
	}

	/**
	 * Evaluates INSERT operations.
	 * Only sends SERVER_ADD if the server is connected and has an IP endpoint.
	 */
	private Optional<ServerEventType> evaluateInsertEvent(GameServer current) {
		if (current == null) {
			log.debug("INSERT event with null current server, ignoring");
			return Optional.empty();
		}

		if (!isServerReadyForProxy(current)) {
			log.info("Game server inserted but not yet connected or has no IP assigned: {}", current.getServerId());
			return Optional.empty();
		}

		return Optional.of(ServerEventType.SERVER_ADD);
	}

	/**
	 * Evaluates UPDATE/REPLACE operations.
	 * Handles connection state changes and endpoint updates.
	 */
	private Optional<ServerEventType> evaluateUpdateEvent(GameServer previous, GameServer current) {
		if (previous == null || current == null) {
			log.info("Received UPDATE/REPLACE event with null previous/current server, ignoring");
			return Optional.empty();
		}

		if (!current.hasChanged(previous)) {
			return Optional.empty();
		}

		// Handle connection state changes
		if (hasConnectionStateChanged(previous, current)) {
			return evaluateConnectionStateChange(current);
		}

		// If the server is not connected, no event needed
		if (!Boolean.TRUE.equals(current.getConnected())) {
			return Optional.empty();
		}

		// Handle endpoint becoming available
		if (hasEndpointBecomeAvailable(previous, current)) {
			return Optional.of(ServerEventType.SERVER_ADD);
		}

		// Other changes while connected require update
		return Optional.of(ServerEventType.SERVER_UPDATE);
	}

	/**
	 * Evaluates DELETE operations.
	 * Only sends SERVER_REMOVE if the server was previously connected.
	 */
	private Optional<ServerEventType> evaluateDeleteEvent(GameServer previous) {
		if (previous != null && Boolean.TRUE.equals(previous.getConnected())) {
			return Optional.of(ServerEventType.SERVER_REMOVE);
		}
		return Optional.empty();
	}

	/**
	 * Checks if the server is ready to be added to proxy routing.
	 */
	private boolean isServerReadyForProxy(GameServer server) {
		return server != null
				&& Boolean.TRUE.equals(server.getConnected())
				&& server.getEndpoint() != null
				&& StringUtils.isNotBlank(server.getEndpoint().getIp());
	}

	/**
	 * Checks if the connection state has changed between previous and current.
	 */
	private boolean hasConnectionStateChanged(GameServer previous, GameServer current) {
		return Boolean.TRUE.equals(previous.getConnected()) == !Boolean.TRUE.equals(current.getConnected());
	}

	/**
	 * Evaluates what event should be sent based on connection state change.
	 */
	private Optional<ServerEventType> evaluateConnectionStateChange(GameServer current) {
		if (Boolean.TRUE.equals(current.getConnected())) {
			return Optional.of(ServerEventType.SERVER_ADD);
		} else {
			return Optional.of(ServerEventType.SERVER_REMOVE);
		}
	}

	/**
	 * Checks if the endpoint became available (was null/empty, now has IP).
	 */
	private boolean hasEndpointBecomeAvailable(GameServer previous, GameServer current) {
		boolean previousHadNoEndpoint = previous.getEndpoint() == null
				|| StringUtils.isEmpty(previous.getEndpoint().getIp());
		boolean currentHasEndpoint = current.getEndpoint() != null
				&& StringUtils.isNotBlank(current.getEndpoint().getIp());

		return previousHadNoEndpoint && currentHasEndpoint;
	}
}
