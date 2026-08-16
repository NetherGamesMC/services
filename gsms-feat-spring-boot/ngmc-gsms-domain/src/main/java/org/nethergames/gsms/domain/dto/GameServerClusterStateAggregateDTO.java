package org.nethergames.gsms.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nethergames.common.domain.dto.IDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;

@Schema(description = "Operator session aggregate view")
public class GameServerClusterStateAggregateDTO implements IDTO {

	@Schema(description = "The game server cluster results with pagination")
	private PagedModel<GameServerClusterStateListDTO> results;

	public GameServerClusterStateAggregateDTO(Page<GameServerClusterStateListDTO> results) {
		this.results = new PagedModel<>(results);
	}

	public PagedModel<GameServerClusterStateListDTO> getResults() {
		return results;
	}

	public void setResults(PagedModel<GameServerClusterStateListDTO> results) {
		this.results = results;
	}
}
