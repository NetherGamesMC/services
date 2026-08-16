package org.nethergames.gsms.app.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.nethergames.gsms.domain.dto.CreateProxyServerDTO;
import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.dto.UpdateProxyServerDTO;
import org.nethergames.gsms.infra.service.ProxyServerCommandService;
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
@Tag(name = "Proxy Server APIs")
@RequestMapping("/gsms/v1/proxies")
public class ProxyServerCommandController {

	private static final Logger log = LoggerFactory.getLogger(ProxyServerCommandController.class);

	private final ProxyServerCommandService commandService;

	public ProxyServerCommandController(ProxyServerCommandService commandService) {
		this.commandService = commandService;
	}

	@RequestMapping(method = RequestMethod.POST)
	@Operation(summary = "Bulk Create Proxy Server",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "Proxy server details to be created"
			),
			responses = @ApiResponse(
					responseCode = "201",
					description = "Proxy server created successfully"
			)
	)
	public ResponseEntity<List<ProxyServerListDTO>> addAll(@RequestBody @NotEmpty List<@Valid CreateProxyServerDTO> dtos) {
		List<ProxyServerListDTO> results = this.commandService.addAll(dtos);
		log.info("Successfully created {} proxy server(s)", results.size());
		return created(null).body(results);
	}

	@RequestMapping(method = RequestMethod.PUT)
	@Operation(summary = "Bulk Update Proxy Server",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "Proxy server details to be updated"
			),
			responses = @ApiResponse(
					responseCode = "200",
					description = "Proxy server updated successfully"
			)
	)
	public ResponseEntity<List<ProxyServerListDTO>> updateAll(@RequestBody @NotEmpty List<@Valid UpdateProxyServerDTO> dtos) {
		List<ProxyServerListDTO> results = this.commandService.updateAll(dtos);
		log.info("Successfully updated {} proxy server(s)", results.size());
		return ok(results);
	}

	@RequestMapping(value = "/{ids}", method = RequestMethod.DELETE)
	@Operation(summary = "Delete Proxy Server",
			parameters = @Parameter(
					required = true,
					name = "ids",
					description = "The collection of proxy server ids"
			),
			responses = @ApiResponse(
					responseCode = "200",
					description = "Proxy servers deleted successfully",
					content = @Content(
							mediaType = MediaType.TEXT_PLAIN_VALUE,
							schema = @Schema(
									implementation = Long.class,
									description = "Number of proxy server(s) deleted"
							)
					)
			)
	)
	public ResponseEntity<Long> deleteAll(@PathVariable @NotEmpty List<String> ids) {
		Long count = commandService.deleteAll(ids);
		log.info("Successfully deleted {} proxy server(s)", count);
		return ok(count);
	}
}
