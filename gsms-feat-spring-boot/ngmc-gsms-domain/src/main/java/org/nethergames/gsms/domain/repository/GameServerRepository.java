package org.nethergames.gsms.domain.repository;

import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.common.mongodb.repo.ExtendedMongoRepository;

import java.util.UUID;

public interface GameServerRepository extends ExtendedMongoRepository<GameServer, UUID>, GameServerRepositoryCustom {

}
