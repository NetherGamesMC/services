package org.nethergames.gsms.app.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.nethergames.gsms.domain.dto.CreateGameServerDTO;
import org.nethergames.gsms.domain.dto.GameServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateGameServerDTO;
import org.nethergames.gsms.infra.service.GameServerCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;

@Validated
@RestController
@Tag(name = "Game Server State APIs")
@RequestMapping("/gsms/v1/game-servers/states")
public class GameServerStateCommandController {

	private static final Logger log = LoggerFactory.getLogger(GameServerStateCommandController.class);

	private final GameServerCommandService commandService;

	public GameServerStateCommandController(GameServerCommandService commandService) {
		this.commandService = commandService;
	}

	@RequestMapping(method = RequestMethod.POST)
	@Operation(summary = "Bulk Create Game Server",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "Game server details to be created"
			),
			responses = @ApiResponse(
					responseCode = "201",
					description = "Game server created successfully"
			)
	)
	public ResponseEntity<List<GameServerListDTO>> addAll(@RequestBody @NotEmpty List<@Valid CreateGameServerDTO> dtos) {
		List<GameServerListDTO> results = this.commandService.addAll(dtos);
		log.info("Successfully created {} game server(s)", results.size());
		return created(null).body(results);
	}

	@RequestMapping(method = RequestMethod.PUT)
	@Operation(summary = "Bulk Update Game Server",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "Game server details to be updated"
			),
			responses = @ApiResponse(
					responseCode = "200",
					description = "Game server updated successfully"
			)
	)
	public ResponseEntity<List<GameServerListDTO>> updateAll(@RequestBody @NotEmpty List<@Valid UpdateGameServerDTO> dtos) {
		List<GameServerListDTO> results = this.commandService.updateAll(dtos);
		log.info("Successfully updated {} game server(s)", results.size());
		return ok(results);
	}

	@RequestMapping(value = "/{ids}", method = RequestMethod.DELETE)
	@Operation(summary = "Delete Game Server",
			parameters = @Parameter(
					required = true,
					name = "ids",
					description = "The collection of game server ids"
			),
			responses = @ApiResponse(
					responseCode = "200",
					description = "Game servers deleted successfully",
					content = @Content(
							mediaType = MediaType.TEXT_PLAIN_VALUE,
							schema = @Schema(
									implementation = Long.class,
									description = "Number of game server(s) deleted"
							)
					)
			)
	)
	public ResponseEntity<Long> deleteAll(@PathVariable @NotEmpty List<String> ids) {
		Long count = commandService.deleteAll(ids);
		log.info("Successfully deleted {} game server(s)", count);
		return ok(count);
	}
}
