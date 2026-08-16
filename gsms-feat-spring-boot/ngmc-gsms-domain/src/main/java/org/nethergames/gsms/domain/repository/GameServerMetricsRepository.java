package org.nethergames.gsms.domain.repository;

import org.nethergames.common.mongodb.repo.ExtendedMongoRepository;
import org.nethergames.gsms.domain.model.GameServerMetrics;
import org.springframework.stereotype.Repository;

@Repository
public interface GameServerMetricsRepository extends ExtendedMongoRepository<GameServerMetrics, String> {

}
