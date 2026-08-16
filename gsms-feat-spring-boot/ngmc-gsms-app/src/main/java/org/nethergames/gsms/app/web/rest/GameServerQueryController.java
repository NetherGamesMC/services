package org.nethergames.gsms.app.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerFilter;
import org.nethergames.gsms.infra.service.GameServerQueryService;
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
@Tag(name = "Game Server APIs")
@RequestMapping("/gsms/v1/game-servers")
public class GameServerQueryController {

	private final GameServerQueryService queryService;

	public GameServerQueryController(GameServerQueryService queryService) {
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
	public ResponseEntity<List<GameServerListDTO>> findAll(@PathVariable @NotEmpty List<String> ids) {
		List<GameServerListDTO> results = queryService.findAll(ids);
		return ok(results);
	}

	@GetMapping
	@Operation(summary = "Get Game Server by Filter",
			responses = @ApiResponse(
					responseCode = "200",
					description = "The list of game servers found by the filter"
			)
	)
	public ResponseEntity<Page<GameServerListDTO>> findAll(
			@NotNull GameServerFilter filter,
			@ParameterObject @PageableDefault(size = 50)
			@SortDefault.SortDefaults({
					@SortDefault(sort = "serverId", direction = Sort.Direction.ASC)
			}) Pageable pageable) {

		Page<GameServerListDTO> results = queryService.findAll(filter, pageable);
		return ok(results);
	}
}
