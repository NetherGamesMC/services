package org.nethergames.gsms.app.config.dbmigrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.model.ScalingDecision;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Date;
import java.util.Map;

@ChangeUnit(order = "3", author = "larryTheCoder", id = "gsms-3-applyScalingDecisionIndexes")
public record ServiceMigration03(MongoTemplate template, MongoDatabase db) {

	@Execution
	public void migrate() {
		MongoCollection<Document> userColl = db.getCollection(ScalingDecision.COLL_NAME);

		userColl.createIndex(
				new Document(Map.of("deployment", 1, "action", 1)),
				new IndexOptions()
						.name("gs_scaling_decision_idx")
						.unique(true)
						.partialFilterExpression(
								new Document("active", true)
						)
		);
	}

	@RollbackExecution
	public void rollback() {
		MongoCollection<Document> userColl = db.getCollection(ScalingDecision.COLL_NAME);
		userColl.dropIndex("gs_scaling_decision_idx");
	}
}
