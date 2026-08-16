package org.nethergames.observer.server.manager;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.nethergames.observer.data.evidences.PunishmentEvidence;
import org.nethergames.observer.data.general.UsernamePunishmentEntry;
import org.nethergames.observer.data.matchmaking.Match;
import org.nethergames.observer.data.matchmaking.MatchParticipation;
import org.nethergames.observer.data.punishment.PlayerComment;
import org.nethergames.observer.data.punishment.Punishment;
import org.nethergames.observer.data.punishment.PunishmentReason;
import org.nethergames.observer.data.punishment.PunishmentReasonGrouped;
import org.nethergames.observer.data.punishment.request.PunishmentSearchData;
import org.nethergames.observer.data.punishment.request.PunishmentSearchRequest;
import org.nethergames.observer.data.reports.PlayerReportEntry;
import org.nethergames.observer.data.tracing.AltTracingDataset;
import org.nethergames.observer.data.tracing.request.TracingWhitelistEntry;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.ParseErrorException;
import org.nethergames.observer.server.generator.RandomIdGenerator;
import org.nethergames.observer.server.generator.TimestampCodec;
import org.nethergames.observer.server.util.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.*;

@Getter
@Log4j2(topic = "MongoManager")
public class MongoManager {
    private static MongoManager manager = null;

    private final MongoCollection<AltTracingDataset> altTracing;
    private final MongoCollection<Punishment> punishmentCollection;
    private final MongoCollection<Match> matchesCollections;
    private final MongoCollection<MatchParticipation> participations;
    private final MongoCollection<PlayerReportEntry> reportsCollections;
    private final MongoCollection<PlayerComment> commentCollections;
    private final MongoCollection<PunishmentReason> reasonCollections;
    private final MongoCollection<TracingWhitelistEntry> whitelistCollections;
    private final MongoCollection<PunishmentEvidence> evidenceCollections;
    private final MongoCollection<UsernamePunishmentEntry> usernameCollections;

    private final MongoClient client;

    public MongoManager() {
        manager = this;

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(fromCodecs(new TimestampCodec()), MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(Configuration.MONGO_URI))
                .codecRegistry(codecRegistry)
                .applyToConnectionPoolSettings(builder -> builder.maxConnecting(50)
                        .maxSize(50)
                        .minSize(15)
                        .maintenanceFrequency(100, TimeUnit.SECONDS)
                        .maintenanceInitialDelay(100, TimeUnit.SECONDS)
                        .maxConnectionIdleTime(50, TimeUnit.SECONDS)
                        .maxConnectionLifeTime(1, TimeUnit.DAYS))
                .build();

        client = MongoClients.create(clientSettings);

        MongoDatabase database = client.getDatabase(Configuration.DATABASE_NAME);
        altTracing = database.getCollection("alt_accounts", AltTracingDataset.class);
        punishmentCollection = database.getCollection("punishments", Punishment.class);
        reportsCollections = database.getCollection("reports", PlayerReportEntry.class);
        matchesCollections = database.getCollection("matches", Match.class);
        participations = database.getCollection("participations", MatchParticipation.class);
        commentCollections = database.getCollection("player_comments", PlayerComment.class);
        reasonCollections = database.getCollection("punishment_reasons", PunishmentReason.class);
        whitelistCollections = database.getCollection("tracing_whitelists", TracingWhitelistEntry.class);
        evidenceCollections = database.getCollection("evidences", PunishmentEvidence.class);
        usernameCollections = database.getCollection("usernames", UsernamePunishmentEntry.class);
    }

    public static void startMigration() {
        MongoCollection<PunishmentEvidence> collection = manager.getEvidenceCollections();

        AtomicBoolean hasEntry = new AtomicBoolean(false);
        collection.find().limit(1).forEach(o -> hasEntry.compareAndSet(false, true));

        if (!hasEntry.get()) {
            log.info("Starting migration...");

            AtomicInteger startedTasks = new AtomicInteger(0);
            manager.getPunishmentCollection().find().forEach(o -> o.getEvidences().forEach(ev -> {
                ev.setPunishmentId(o.getId());
                ev.setPlayer(o.getXuid());

                addEvidence(ev);

                int numOfEvidences = startedTasks.getAndIncrement();

                if (numOfEvidences > 0 && numOfEvidences % 50 == 0) {
                    log.info("Processed {} evidences so far...", numOfEvidences);
                }
            }));

            // use Observer
            // db.punishments.updateMany({}, [{ $unset: ["evidence"] }])

            log.info("Evidence migration completed.");
        }
    }

    public static List<PunishmentReason> getPunishmentReasons(boolean publicFlag) {
        FindIterable<PunishmentReason> result;

        if (publicFlag) {
            result = manager.getReasonCollections().find(not(eq("internalOnly", true)));
        } else {
            result = manager.getReasonCollections().find();
        }

        ArrayList<PunishmentReason> reasons = new ArrayList<>();
        result.forEach(reasons::add);

        return reasons;
    }

    public static PunishmentReasonGrouped getGroupedPunishmentReasons(boolean publicFlag) {
        PunishmentReasonGrouped reasons = new PunishmentReasonGrouped(new LinkedHashMap<>());

        getPunishmentReasons(publicFlag).forEach((reason) -> {
            List<PunishmentReason> punishmentList = reasons.getReasons().get(reason.getCategory());

            if (punishmentList == null) {
                List<PunishmentReason> list = new ArrayList<>();
                list.add(reason);

                reasons.getReasons().put(reason.getCategory(), list);
            } else {
                punishmentList.add(reason);
            }
        });

        return reasons;
    }

    public static void insertPunishment(PunishmentReason reason) {
        manager.getReasonCollections().insertOne(reason);
    }

    public static PunishmentReason getPunishmentReason(String name) {
        return manager.getReasonCollections().find(and(eq("name", name))).first();
    }

    public static boolean updatePunishmentReason(String reasonName, PunishmentReason reason) {
        UpdateResult result;
        try {
            result = manager.getReasonCollections().replaceOne(and(eq("name", reasonName)), reason);
        } catch (Throwable t) {
            result = null;
        }

        if (result != null && result.getModifiedCount() > 0) {
            manager.getPunishmentCollection().updateMany(and(eq("reason.name", reasonName)), set("reason", reason));

            return true;
        }

        return false;
    }

    public static boolean deletePunishmentReason(String reasonName) {
        DeleteResult result;

        try {
            result = manager.getReasonCollections().deleteOne(and(eq("name", reasonName)));
        } catch (Throwable t) {
            result = null;
        }

        return result != null && result.getDeletedCount() > 0;
    }

    public static boolean updatePunishment(Punishment punishment) {
        UpdateResult result;

        try {
            result = manager.getPunishmentCollection().replaceOne(eq(Punishment.DATABASE_IDENTIFIER, punishment.getId()), punishment);
        } catch (Throwable t) {
            result = null;
        }

        return result != null && result.getModifiedCount() > 0;
    }

    public static void insertPunishment(@NotNull Punishment punishment) {
        String randomId = RandomIdGenerator.generate();
        punishment.setId(randomId);

        InsertOneResult result;
        try {
            result = manager.getPunishmentCollection().insertOne(punishment);
        } catch (Throwable t) {
            result = null;
        }

        if (result == null || result.getInsertedId() == null) {
            insertPunishment(punishment); // Retry with a different punishment id
        }
    }

    public static void addEvidence(PunishmentEvidence evidence) {
        manager.getEvidenceCollections().insertOne(evidence);
    }

    public static boolean deleteEvidence(PunishmentEvidence evidence) {
        DeleteResult result = manager.getEvidenceCollections().deleteOne(
                and(
                        eq("punishmentId", evidence.getPunishmentId()),
                        eq("evidenceId", evidence.getEvidenceId())
                )
        );

        return result.getDeletedCount() > 0;
    }

    public static void addUsernameEntry(UsernamePunishmentEntry username) {
        manager.getUsernameCollections().insertOne(username);
    }

    public static void deleteUsernameEntry(String playerXuid) {
        manager.getUsernameCollections().deleteOne(eq("xuid", playerXuid));
    }

    public static Punishment getPunishment(String id) {
        return manager.getPunishmentCollection().find(eq(Punishment.DATABASE_IDENTIFIER, id)).first();
    }

    public static boolean deletePunishment(String id) {
        DeleteResult result;

        try {
            result = manager.getPunishmentCollection().deleteOne(eq(Punishment.DATABASE_IDENTIFIER, id));
        } catch (Throwable t) {
            result = null;
        }

        return result != null && result.getDeletedCount() > 0;
    }

    public static void deletePunishmentByReason(String reasonName) {
        manager.getPunishmentCollection().deleteOne(and(eq("reason.name", reasonName)));
    }

    public static AltTracingDataset getSingleDataset(String xuid) {
        FindIterable<AltTracingDataset> result = manager.getAltTracing().find(eq("xuid", xuid));

        return result.first();
    }

    public static PunishmentSearchData searchPunishment(PunishmentSearchRequest searchFor, long nextOffset, boolean resolveEvidences) {
        List<Bson> filters = new ArrayList<>();
        if (searchFor.getTargetXuid() != null) {
            filters.add(eq("xuid", searchFor.getTargetXuid()));
        }

        if (nextOffset > 0) {
            filters.add(lt("issuedAt", nextOffset));
        }

        if (searchFor.getIssuerXuid() != null) {
            filters.add(eq("issuedBy", searchFor.getIssuerXuid()));
        }

        if (searchFor.getAfterIssued() != null) {
            filters.add(gt("issuedAt", Instant.ofEpochMilli(searchFor.getAfterIssued().getTime()).getEpochSecond()));
        }

        if (searchFor.getBeforeIssued() != null) {
            if (searchFor.getAfterIssued() != null && searchFor.getAfterIssued().getTime() >= searchFor.getBeforeIssued().getTime()) {
                throw new ParseErrorException("'before' cannot be less or equal than 'after'");
            }

            filters.add(lt("issuedAt", Instant.ofEpochMilli(searchFor.getBeforeIssued().getTime()).getEpochSecond()));
        }

        if (searchFor.getCategory() != null) {
            filters.add(eq("reason.category", searchFor.getCategory()));
        }

        if (searchFor.getEvidenceType() != null && searchFor.getEvidenceType() != PunishmentSearchRequest.EvidenceType.ALL) {
            filters.add(exists("evidences", searchFor.getEvidenceType() == PunishmentSearchRequest.EvidenceType.ONLY_SUBMITTED));
        }

        var offset = new PunishmentSearchData();
        var list = new ArrayList<Punishment>();

        if(filters.size() > 0) {
            Observer.getObserver().getMongoManager().getPunishmentCollection().find(and(filters)).limit(50).sort(Sorts.descending("issuedAt")).forEach(list::add);
        }else {
            Observer.getObserver().getMongoManager().getPunishmentCollection().find().limit(50).sort(Sorts.descending("issuedAt")).forEach(list::add);
        }


        if(list.size() > 0) {
            offset.setCurrentOffset(list.get(0).getIssuedAt());
        }


        if (list.size() == 50) {
            offset.setNextOffset(list.get(list.size() - 1).getIssuedAt());
        }

        if (resolveEvidences)
            list.forEach(d -> d.setEvidences(Observer.getObserver().getEvidenceManager().getEvidences(d.getId())));

        offset.setPunishments(list);

        return offset;
    }
}
