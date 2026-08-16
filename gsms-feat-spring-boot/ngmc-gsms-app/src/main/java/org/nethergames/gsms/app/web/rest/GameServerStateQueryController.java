package org.nethergames.gsms.app.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.nethergames.gsms.domain.dto.GameServerClusterStateAggregateDTO;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.nethergames.gsms.infra.service.GameServerStateQueryService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@Validated
@RestController
@Tag(name = "Game Server State APIs")
@RequestMapping("/gsms/v1/game-servers/states")
public class GameServerStateQueryController {

	private final GameServerStateQueryService queryService;

	public GameServerStateQueryController(GameServerStateQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping(value = "/{ids}")
	@Operation(summary = "Get Game Server by ids",
			parameters = @Parameter(
					required = true,
					name = "ids",
					description = "The collection of game server ids"
			),
			responses = @ApiResponse(
					responseCode = "200",
					description = "The list of game servers found by the collection of ids"
			)
	)
	public ResponseEntity<List<GameServerStateListDTO>> findAll(@PathVariable @NotEmpty List<String> ids) {
		List<GameServerStateListDTO> results = queryService.findAll(ids);
		return ok(results);
	}

	@GetMapping
	@Operation(summary = "Get Game Server by Filter",
			responses = @ApiResponse(
					responseCode = "200",
					description = "The list of game servers found by the filter"
			)
	)
	public ResponseEntity<Page<GameServerStateListDTO>> findAll(
			@NotNull GameServerStateFilter filter,
			@ParameterObject @PageableDefault(size = 50)
			@SortDefault.SortDefaults({
					@SortDefault(sort = "serverId", direction = Sort.Direction.ASC)
			}) Pageable pageable) {

		Page<GameServerStateListDTO> results = queryService.findAll(filter, pageable);
		return ok(results);
	}

	@GetMapping("/clusters")
	@Operation(summary = "Get Game Server clusters by Filter",
			responses = @ApiResponse(
					responseCode = "200",
					description = "The list of game server clusters found by the filter"
			)
	)
	public ResponseEntity<GameServerClusterStateAggregateDTO> findAllClusters(
			@NotNull GameServerStateFilter filter,
			@ParameterObject @PageableDefault(size = 50)
			@SortDefault.SortDefaults({
					@SortDefault(sort = "gameType", direction = Sort.Direction.ASC)
			}) Pageable pageable) {

		GameServerClusterStateAggregateDTO results = queryService.findAllClusters(filter, pageable);
		return ok(results);
	}
}
