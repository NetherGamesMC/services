package org.nethergames.gsms.domain.repository;

import org.nethergames.common.mongodb.repo.ExtendedMongoRepository;
import org.nethergames.gsms.domain.model.ProxyServer;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProxyServerRepository extends ExtendedMongoRepository<ProxyServer, UUID> {

}
