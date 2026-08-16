package org.nethergames.gsms.app.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.nethergames.gsms.domain.dto.ProxyServerListDTO;
import org.nethergames.gsms.domain.model.filter.ProxyServerFilter;
import org.nethergames.gsms.infra.service.ProxyServerQueryService;
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
@Tag(name = "Proxy Server APIs")
@RequestMapping("/gsms/v1/proxies")
public class ProxyServerQueryController {

	private final ProxyServerQueryService queryService;

	public ProxyServerQueryController(ProxyServerQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping(value = "/{ids}")
	@Operation(summary = "Get Proxy Server by ids",
			parameters = @Parameter(
					required = true,
					name = "ids",
					description = "The collection of proxy server ids"
			),
			responses = @ApiResponse(
					responseCode = "200",
					description = "The list of proxy servers found by the collection of ids"
			)
	)
	public ResponseEntity<List<ProxyServerListDTO>> findAll(@PathVariable @NotEmpty List<String> ids) {
		List<ProxyServerListDTO> results = queryService.findAll(ids);
		return ok(results);
	}

	@GetMapping
	@Operation(summary = "Get Proxy Server by Filter",
			responses = @ApiResponse(
					responseCode = "200",
					description = "The list of proxy servers found by the filter"
			)
	)
	public ResponseEntity<Page<ProxyServerListDTO>> findAll(
			@NotNull ProxyServerFilter filter,
			@ParameterObject @PageableDefault(size = 50)
			@SortDefault.SortDefaults({
					@SortDefault(sort = "serverId", direction = Sort.Direction.ASC)
			}) Pageable pageable) {

		Page<ProxyServerListDTO> results = queryService.findAll(filter, pageable);
		return ok(results);
	}
}
