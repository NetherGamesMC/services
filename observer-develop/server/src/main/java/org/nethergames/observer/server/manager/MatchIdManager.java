package org.nethergames.observer.server.manager;

import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.conversions.Bson;
import org.nethergames.observer.data.matchmaking.Match;
import org.nethergames.observer.data.matchmaking.MatchParticipation;
import org.nethergames.observer.data.matchmaking.TrackingEvent;
import org.nethergames.observer.data.matchmaking.request.StartGameRequest;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.generator.RandomIdGenerator;
import org.nethergames.observer.server.exception.MatchNotFoundException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;

public class MatchIdManager {

    public List<TrackingEvent> getEventsForId(String trackingId) {
        FindIterable<Match> eventResult = Observer.getObserver().getMongoManager().getMatchesCollections().find(eq("id", trackingId));

        Match match = eventResult.first();

        if (match == null) {
            throw new MatchNotFoundException(trackingId);
        }

        return match.getEvents();
    }

    public void registerMatchParticipation(MatchParticipation participation) {
        Observer.getObserver().getMongoManager().getParticipations().insertOne(participation);
    }

/*    private List<MatchParticipation> filterParticipations(String key, String value) {
        Session session = this.observer.getHibernateSessionFactory().openSession();

        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<MatchParticipation> criteriaQuery = builder.createQuery(MatchParticipation.class);
        Root<MatchParticipation> root = criteriaQuery.from(MatchParticipation.class);

        criteriaQuery.select(root).where(builder.equal(root.get(key), value));

        Query<MatchParticipation> query = session.createQuery(criteriaQuery);
        List<MatchParticipation> participations = query.list();

        session.close();

        return participations;
    }
*/
    /*public List<MatchParticipation> filterParticipationForTime(String key, String value) {
        Session session = this.observer.getHibernateSessionFactory().openSession();

        session.beginTransaction();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<MatchParticipation> criteriaQuery = builder.createQuery(MatchParticipation.class);
        Root<MatchParticipation> root = criteriaQuery.from(MatchParticipation.class);
        criteriaQuery.select(root).where(builder.equal(root.get(key), value));


        Query<MatchParticipation> query = session.createQuery(criteriaQuery);
        List<MatchParticipation> participations = query.list();

        session.close();

        return participations;
    }*/

    public Match getMatchWithId(String matchId) {
        FindIterable<Match> result = Observer.getObserver().getMongoManager().getMatchesCollections().find(eq("id", matchId));

        Match first = result.first();

        if (first == null) {
            throw new MatchNotFoundException(matchId);
        }

        return result.first();
    }

    public List<MatchParticipation> filterParticipationsForPlayerAfter(String xuid, Timestamp before) {
        Bson query = Filters.and(
                eq("xuid", xuid),
                gt("startedAt", before.getTime())
        );

        FindIterable<MatchParticipation> participations = Observer.getObserver().getMongoManager().getParticipations().find(query);

        ArrayList<MatchParticipation> participationList = new ArrayList<>();

        participations.forEach(participationList::add);

        return participationList;
    }

    public void finalizeMatch(String matchId) {

        long currentTime = System.currentTimeMillis();

        Bson updates = Updates.set("endedAt", currentTime);

        UpdateResult result = Observer.getObserver().getMongoManager().getMatchesCollections().updateOne(eq("id", matchId), updates);

        if (result.getMatchedCount() == 0) {
            throw new MatchNotFoundException(matchId);
        }
    }

    public Match startMatchTracking(StartGameRequest request) {
        Match match = new Match();
        match.setId(RandomIdGenerator.generate());
        match.setServerType(request.getServerType());
        match.setGameType(request.getGameType());
        match.setMap(request.getMap());

        try {
            Observer.getObserver().getMongoManager().getMatchesCollections().insertOne(match);
        } catch (MongoWriteException exception) {
            startMatchTracking(request);
        }
        return match;
    }


    /*
    public List<MatchParticipation> getParticipationsFor(Match match) {
        return this.filterParticipations("matchId", match.getId());
    }*/

    public List<MatchParticipation> getMatchParticipations(String matchId) {
        FindIterable<MatchParticipation> matchResult = Observer.getObserver().getMongoManager().getParticipations().find(eq("id", matchId));

        ArrayList<MatchParticipation> list = new ArrayList<>();
        matchResult.forEach(list::add);

        return list;
    }

    /*
    public List<MatchParticipation> getPlayersParticipations(String xuid) {
        return this.filterParticipations("xuid", xuid);
    }*/
}
