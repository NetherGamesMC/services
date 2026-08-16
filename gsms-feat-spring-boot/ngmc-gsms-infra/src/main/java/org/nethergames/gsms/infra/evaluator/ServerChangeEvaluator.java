package org.nethergames.gsms.infra.evaluator;

import com.mongodb.client.model.changestream.OperationType;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.rpc.ServerEventType;

import java.util.Optional;

public interface ServerChangeEvaluator {

	Optional<ServerEventType> determineEventType(OperationType operation, GameServer previous, GameServer current);
}
