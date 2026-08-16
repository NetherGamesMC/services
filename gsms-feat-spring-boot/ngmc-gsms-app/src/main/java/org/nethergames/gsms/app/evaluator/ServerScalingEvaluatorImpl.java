package org.nethergames.gsms.app.evaluator;

import org.nethergames.common.domain.model.values.GameServerMetadata;
import org.nethergames.gsms.domain.dto.GameServerStateListDTO;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.nethergames.gsms.domain.model.values.ScalingScope;
import org.nethergames.gsms.infra.evaluator.ServerScalingEvaluator;
import org.nethergames.gsms.infra.service.GameServerStateQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Component
public class ServerScalingEvaluatorImpl implements ServerScalingEvaluator {

	private final Map<ScalingScope, ScheduledFuture<?>> pending = new ConcurrentHashMap<>();

	private final ScheduledExecutorService executor;

	private final GameServerStateQueryService queryService;

	private Random random;

	public ServerScalingEvaluatorImpl(@Qualifier("virtualTaskExecutor") ScheduledExecutorService executor, GameServerStateQueryService queryService) {
		this.executor = executor;
		this.queryService = queryService;
	}

	@PostConstruct
	public void init() {
		this.random = Random.from(RandomGenerator.getDefault());
	}

	@Override
	public void markDirty(ScalingScope scope) {
		ScheduledFuture<?> schedule = executor.schedule(() -> {
			try {

			} finally {
				pending.remove(scope);
			}
		}, random.nextInt(10, 30), TimeUnit.SECONDS);

		pending.putIfAbsent(scope, schedule);
	}

	private void evaluate(ScalingScope scope) {
		GameServerStateFilter filter = new GameServerStateFilter();
		filter.setServerRegions(List.of(scope.getServerRegion()));
		filter.setServerTypes(List.of(scope.getServerType()));
		filter.setGameTypes(List.of(scope.getGameType()));

		Page<GameServerStateListDTO> servers = queryService.findAll(filter, Pageable.unpaged());

		// Calculate pressure score scaling model from server metrics
		Map<GameServerMetadata, Double> pressureScores = servers.stream().collect(Collectors.toMap(GameServerStateListDTO::getMetadata,
				value -> {
					float tpsPressure = Math.clamp((20 - value.getLastTps()) / 20, 0, 1);
					float cpuPressure = Math.clamp(value.getLastUsage() / 100, 0, 1);
					float memoryPressure = Math.clamp(value.getLastMemoryUsage() / 1800, 0, 1);

					return (tpsPressure * 0.5) + (cpuPressure * 0.3) + (memoryPressure * 0.2);
				}
		));

		int currentInstances = pressureScores.size();
		if (currentInstances > 1) {
			// There are more than 1 active instance for this server, if the score is below 0.3, then it is underutilized.
			double scoreAvg = pressureScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
			if (scoreAvg < 0.3) {
				// TODO: Logic
			}
		}

	}
}
