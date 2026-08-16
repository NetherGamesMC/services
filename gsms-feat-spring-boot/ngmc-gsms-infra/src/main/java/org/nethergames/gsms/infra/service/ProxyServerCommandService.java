package org.nethergames.gsms.infra.service;

import org.nethergames.gsms.domain.dto.CreateProxyServerDTO;
import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateProxyServerDTO;

import java.util.List;

public interface ProxyServerCommandService {
	List<ProxyServerListDTO> addAll(Iterable<CreateProxyServerDTO> dtos);

	List<ProxyServerListDTO> updateAll(Iterable<UpdateProxyServerDTO> dtos);

	long deleteAll(Iterable<String> ids);

}
