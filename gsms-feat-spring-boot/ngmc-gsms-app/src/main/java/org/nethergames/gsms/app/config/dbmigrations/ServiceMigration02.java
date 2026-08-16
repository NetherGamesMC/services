package org.nethergames.gsms.app.config.dbmigrations;

import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.nethergames.gsms.domain.model.GameServer;
import org.nethergames.gsms.domain.model.GameServerState;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(order = "2", author = "larryTheCoder", id = "gsms-2-applyServerForMongoStreams")
public record ServiceMigration02(MongoTemplate template, MongoDatabase db) {

	@Execution
	public void migrate() {
		db.runCommand(new Document("collMod", GameServer.COLL_NAME)
				.append("changeStreamPreAndPostImages", new Document("enabled", true)));
	}

	@RollbackExecution
	public void rollback() {
		db.runCommand(new Document("collMod", GameServer.COLL_NAME)
				.append("changeStreamPreAndPostImages", new Document("enabled", false)));
	}
}
