package org.nethergames.gsms.domain.repository;

import org.nethergames.common.mongodb.repo.ExtendedMongoRepository;
import org.nethergames.gsms.domain.model.GameServerState;
import org.springframework.stereotype.Repository;

import java.util.UUID;

public interface GameServerStateRepository extends ExtendedMongoRepository<GameServerState, UUID>, GameServerStateRepositoryCustom {

}
