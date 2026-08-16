package org.nethergames.gsms.infra.repository;

import com.mongodb.BasicDBObject;
import org.nethergames.gsms.domain.dto.GameServerClusterStateAggregateDTO;
import org.nethergames.gsms.domain.dto.GameServerClusterStateAggregateFacetDTO;
import org.nethergames.gsms.domain.dto.GameServerClusterStateListDTO;
import org.nethergames.gsms.domain.model.GameServerState;
import org.nethergames.gsms.domain.model.filter.GameServerStateFilter;
import org.nethergames.gsms.domain.repository.GameServerStateRepositoryCustom;
import org.nethergames.gsms.domain.specs.GameServerStateSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class GameServerStateRepositoryImpl implements GameServerStateRepositoryCustom {
	private static final Logger log = LoggerFactory.getLogger(GameServerStateRepositoryImpl.class);

	private final MongoTemplate mongoTemplate;

	public GameServerStateRepositoryImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public GameServerClusterStateAggregateDTO findAll(GameServerStateFilter filter, Pageable pageable) {
		List<AggregationOperation> aggregateOperations = new ArrayList<>();
		GameServerStateSpecification spec = new GameServerStateSpecification(filter);
		List<CriteriaDefinition> criterias = spec.toCriteria();
		if (!criterias.isEmpty()) {
			//noinspection SuspiciousToArrayCall
			Criteria criteria0 = new Criteria().andOperator(criterias.toArray(new Criteria[0]));
			aggregateOperations.add(Aggregation.match(criteria0));
		}

		aggregateOperations.add(Aggregation.group("metadata.gameType", "metadata.serverType")
				.push(new BasicDBObject("serverId", "$serverId")
						.append("maxPlayers", "$maxPlayerCount")
						.append("totalPlayers", "$playerCount")
						.append("tps", "$lastTps")
						.append("usage", "$lastUsage")
				).as("clusterData")
				.avg("lastTps").as("avgTps")
				.sum("playerCount").as("totalPlayers")
				.sum("maxPlayerCount").as("maxPlayers"));

		aggregateOperations.add(Aggregation.project("clusterData", "avgTps", "totalPlayers", "maxPlayers")
				.andExclude("_id")
				.and("_id.gameType").as("gameType")
				.and("_id.serverType").as("serverType"));

		if (pageable.isUnpaged()) {
			MatchOperation allRecords = Aggregation.match(new Criteria());
			CountOperation countStage = Aggregation.count().as("count");

			FacetOperation facetOp = Aggregation.facet(allRecords).as("results")
					.and(countStage).as("totalCount");
			aggregateOperations.add(facetOp);
		} else {
			SkipOperation skipStage = Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize());
			LimitOperation limitStage = Aggregation.limit(pageable.getPageSize());
			SortOperation sortStage = Aggregation.sort(pageable.getSort());
			CountOperation countStage = Aggregation.count().as("count");

			FacetOperation facetOp1 = Aggregation.facet(sortStage, skipStage, limitStage).as("results")
					.and(countStage).as("totalCount");
			aggregateOperations.add(facetOp1);
		}

		Aggregation aggregation = Aggregation.newAggregation(aggregateOperations);
		AggregationResults<GameServerClusterStateAggregateFacetDTO> output = mongoTemplate.aggregate(aggregation, GameServerState.COLL_NAME, GameServerClusterStateAggregateFacetDTO.class);
		GameServerClusterStateAggregateFacetDTO mappedResults = output.getMappedResults().isEmpty() ? null
				: output.getMappedResults().getFirst();

		List<GameServerClusterStateListDTO> dtos = null == mappedResults ? Collections.emptyList() : mappedResults.getResults();
		long totalCount = null == mappedResults ? 0L
				: null == mappedResults.getTotalCount() || mappedResults.getTotalCount().isEmpty() ? 0L
				: mappedResults.getTotalCount().getFirst().getCount();

		if (null == dtos) {
			dtos = Collections.emptyList();
			totalCount = 0;
		}

		// We can add statistics if we want to in the future.
		Page<GameServerClusterStateListDTO> page = new PageImpl<>(dtos, pageable, totalCount);
		return new GameServerClusterStateAggregateDTO(page);
	}
}
