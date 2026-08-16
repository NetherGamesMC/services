package org.nethergames.observer.server.manager;

import com.mongodb.client.model.Filters;
import lombok.extern.log4j.Log4j2;
import org.bson.conversions.Bson;
import org.nethergames.observer.data.general.UsernamePunishmentEntry;
import org.nethergames.observer.data.metadata.PlayerAddressRequestList;
import org.nethergames.observer.data.metadata.PlayerAddressResponseMap;
import org.nethergames.observer.data.tracing.AltTracingDataset;
import org.nethergames.observer.data.tracing.AltTracingPushData;
import org.nethergames.observer.data.tracing.request.TracingSearchEntry;
import org.nethergames.observer.data.tracing.request.TracingSearchResult;
import org.nethergames.observer.data.tracing.request.TracingWhitelistEntry;
import org.nethergames.observer.data.tracing.type.TracingType;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.SearchErrorException;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.ascending;

@Log4j2(topic = "TracingManager")
public class TracingManager {
    private final Map<String, Set<String>> tracingWhitelist;

    public TracingManager(Observer observer) {
        tracingWhitelist = new HashMap<>();

        observer.getMongoManager().getWhitelistCollections().find().forEach(o -> {
            if (tracingWhitelist.containsKey(o.getOriginXuid())) {
                tracingWhitelist.get(o.getOriginXuid()).add(o.getExclusionXuid());
            } else {
                tracingWhitelist.put(o.getOriginXuid(), new LinkedHashSet<>(Set.of(o.getExclusionXuid())));
            }
        });
    }

    public void updateDataset(AltTracingPushData data) {
        MongoManager mongoManager = Observer.getObserver().getMongoManager();
        AltTracingDataset currentDataset = MongoManager.getSingleDataset(data.getXuid());

        // Entry dataset.
        if (currentDataset == null) {
            currentDataset = new AltTracingDataset(
                    data.getXuid(),
                    Collections.singleton(data.getIp()),
                    Collections.singleton(data.getDeviceId()),
                    Collections.singleton(data.getSelfSignedId())
            );

            mongoManager.getAltTracing().insertOne(currentDataset);
        } else {
            boolean changed = false;
            if (data.getIp() != null) {
                changed = currentDataset.getIp().add(data.getIp());
            }
            if (data.getDeviceId() != null) {
                changed = (currentDataset.getDeviceId().add(data.getDeviceId()) || changed);
            }
            if (data.getSelfSignedId() != null) {
                changed = (currentDataset.getSelfSignedId().add(data.getSelfSignedId()) || changed);
            }

            if (changed) {
                mongoManager.getAltTracing().replaceOne(eq("xuid", data.getXuid()), currentDataset);
            }
        }

        // TODO:
        // IP dataset.
        // This dataset defines the IP -> user by a specified timeframe (Which will expire in 2 weeks).


        // Username dataset.
        // Removes "Inappropriate username" ban if the player changed their username.
        UsernamePunishmentEntry username = mongoManager.getUsernameCollections().find(eq("xuid", data.getXuid())).first();
        if (username != null && data.getUsername() != null && !username.getCurrentUsername().equalsIgnoreCase(data.getUsername())) {
            MongoManager.deletePunishment(username.getCurrentPunishment());
            MongoManager.deleteUsernameEntry(username.getXuid());
        }
    }

    /**
     * Search for an alt accounts for the given xuid list. The depth for each search is determined
     * by the given depth parameter, the returned results is always ordered from depth 1 to max.
     *
     * @param searchDataset The dataset for the recursive search algorithm.
     * @return The result for the computation. Map of xuid -> {Depth -> [Map of xuids to what matches it]}
     */
    public Map<String, List<TracingSearchResult>> recursiveSearch(TracingSearchEntry searchDataset) {
        Map<String, List<TracingSearchResult>> results = new HashMap<>();

        searchDataset.getXuidList().forEach(xuid -> {
            int currentDepth = 0;

            // Exclude these xuids from our search.
            var exclusions = searchDataset.getExclusions();
            if (tracingWhitelist.containsKey(xuid)) {
                exclusions.addAll(tracingWhitelist.get(xuid));
            }

            // Consider this as level 0, the root for our tree.
            // The root is a xuid, the child for a xuid's root is DeviceIds, IPs, and SelfSignedIds.
            // In this dataset, it will include related xuid *and* their related DID, IP, and SSID
            var tracingSet = MongoManager.getSingleDataset(xuid);
            if (tracingSet == null) {
                results.put(xuid, new ArrayList<>());
                return;
            }

            var dataset = getDataset(Set.of(tracingSet), searchDataset);
            var nextDataset = new LinkedHashSet<>(dataset);

            dataset.add(tracingSet);

            var currentTrace = flattenToMap(dataset, tracingSet, searchDataset.getSearchConditions());
            var resultTraced = new HashMap<>(Map.of(currentDepth, currentTrace));

            // During recursive search, we want to find *all* dataset related to DID, IP and SSID.
            // Here, we are trying to receive all dataset related to those set we did above - then replaces
            // with the new one, it is recursive.
            while (currentDepth < searchDataset.getDepth() && !currentTrace.isEmpty()) {
                var newDataset = getDataset(nextDataset, searchDataset);

                // Remove all duplicates in our new dataset (that was found in the higher level).
                newDataset.removeAll(dataset.stream().filter(newDataset::contains).collect(Collectors.toUnmodifiableSet()));
                nextDataset.clear();
                nextDataset.addAll(newDataset);

                // The new set of related xuids.
                currentTrace = flattenToMap(newDataset, tracingSet, searchDataset.getSearchConditions());

                // Map the traced result by { depth -> trace }
                resultTraced.put(++currentDepth, currentTrace);
                dataset.addAll(newDataset);
            }

            var list = new ArrayList<TracingSearchResult>();
            resultTraced.forEach((depth, data) -> list.add(new TracingSearchResult(depth, data)));

            results.put(xuid, list);
        });

        return results;
    }

    private static Map<TracingType, Set<String>> flattenToMap(Set<AltTracingDataset> dataset, AltTracingDataset origin, EnumSet<TracingType> conditions) {
        Map<TracingType, Set<String>> traces = new HashMap<>();
        Arrays.stream(TracingType.values()).forEach(o -> traces.put(o, new TreeSet<>()));

        var ip = traces.get(TracingType.IP);
        var did = traces.get(TracingType.DEVICE_ID);
        var ssid = traces.get(TracingType.SELF_SIGNED_ID);

        dataset.forEach(o -> conditions.forEach(searchType -> {
            switch (searchType) {
                case IP -> o.getIp().stream()
                        .filter(origin.getIp()::contains)
                        .forEach(v -> ip.add(o.getXuid()));
                case DEVICE_ID -> o.getDeviceId().stream()
                        .filter(origin.getDeviceId()::contains)
                        .forEach(v -> did.add(o.getXuid()));
                case SELF_SIGNED_ID -> o.getSelfSignedId().stream()
                        .filter(origin.getSelfSignedId()::contains)
                        .forEach(v -> ssid.add(o.getXuid()));
            }
        }));

        return traces;
    }

    /**
     * Attempt to unlink a player from the actor, the processes will eliminate all related
     * device ids, self-signed ids, and IP addresses by this actor to the player.
     *
     * @param xuid  The player xuid (The recipient in which would want to unlink from actor).
     * @param actor The actor (target of which we would want to refer).
     * @return A list of integers counting matches of two dataset that was removed.
     */
    public ArrayList<Integer> unlinkPlayerTrace(String xuid, String actor) {
        var db = Observer.getObserver().getMongoManager().getAltTracing();

        // First step is to fetch the actor traces.
        var currentDataset = db.find(in("xuid", xuid), AltTracingDataset.class).first();
        var actorDataset = db.find(in("xuid", actor), AltTracingDataset.class).first();

        if (currentDataset == null || actorDataset == null) {
            return null;
        }

        var result = new ArrayList<>(Arrays.asList(
                removeAll(currentDataset.getDeviceId().iterator(), actorDataset.getDeviceId().stream().toList()),
                removeAll(currentDataset.getSelfSignedId().iterator(), actorDataset.getSelfSignedId().stream().toList()),
                removeAll(currentDataset.getIp().iterator(), actorDataset.getIp().stream().toList())
        ));

        db.replaceOne(eq("xuid", currentDataset.getXuid()), currentDataset);

        return result;
    }

    /**
     * Return any data related to the dataset given as a parameter, the search dataset is used to identify
     * which conditions such relation should be (by IP, by deviceID, by selfSignedId) and its exclusions.
     *
     * @param dataset       The dataset for such searches.
     * @param searchDataset The search dataset for conditions and exclusions.
     * @return All related alt traces in which is related to the parameter given.
     */
    public Set<AltTracingDataset> getDataset(Iterable<AltTracingDataset> dataset, TracingSearchEntry searchDataset) {
        if (searchDataset.getSearchConditions().isEmpty()) {
            throw new SearchErrorException();
        }

        Set<String> ip = new LinkedHashSet<>();
        Set<String> deviceIds = new LinkedHashSet<>();
        Set<String> selfSignedIds = new LinkedHashSet<>();
        for (AltTracingDataset altTracingDataset : dataset) {
            cleanFromOverlapData(altTracingDataset);

            ip.addAll(altTracingDataset.getIp());
            deviceIds.addAll(altTracingDataset.getDeviceId());
            selfSignedIds.addAll(altTracingDataset.getSelfSignedId());
        }

        // Trace for any matches for the given dataset using the conditions set in the search dataset parameter.
        Set<Bson> searchCondition = new LinkedHashSet<>();
        for (TracingType type : searchDataset.getSearchConditions()) {
            switch (type) {
                case IP -> searchCondition.add(in("ip", ip));
                case DEVICE_ID -> searchCondition.add(in("deviceId", deviceIds));
                case SELF_SIGNED_ID -> searchCondition.add(in("selfSignedId", selfSignedIds));
            }
        }

        if (searchCondition.isEmpty()) {
            return Set.of();
        }

        Set<AltTracingDataset> datasets = new LinkedHashSet<>();
        // try-with-resources and a cursor should maybe help with allocations here
        try (var tracedDataset = Observer.getObserver().getMongoManager().getAltTracing().find(
                and(
                        not(
                                in("xuid", searchDataset.getExclusions())
                        ),
                        Filters.or(searchCondition)
                ), AltTracingDataset.class
        ).sort(ascending("xuid")).iterator()) {
            while (tracedDataset.hasNext()) {
                datasets.add(tracedDataset.next());
            }
        }


        return datasets;
    }

    public PlayerAddressResponseMap getAddressDatasetFor(PlayerAddressRequestList requestList) {
        PlayerAddressResponseMap response = new PlayerAddressResponseMap();

        Observer.getObserver().getMongoManager().getAltTracing()
                .find(in("xuid", requestList), AltTracingDataset.class)
                .forEach(dataset -> response.put(dataset.getXuid(), new ArrayList<>(dataset.getIp())));

        return response;
    }

    public void addWhitelistEntry(TracingWhitelistEntry entry) {
        if (tracingWhitelist.containsKey(entry.getOriginXuid())) {
            tracingWhitelist.get(entry.getOriginXuid()).add(entry.getExclusionXuid());
        } else {
            tracingWhitelist.put(entry.getOriginXuid(), new LinkedHashSet<>(Set.of(entry.getExclusionXuid())));
        }

        Observer.getObserver().getMongoManager().getWhitelistCollections().insertOne(entry);
    }

    /**
     * There were internal ip addresses in the old data, which were the same for tons of players. This is
     * to prevent these from being included
     *
     * @param dataset The AltTracingDataset
     */
    private void cleanFromOverlapData(AltTracingDataset dataset) {
        dataset.getIp().removeIf(ip -> ip.startsWith("172.18"));
        dataset.getIp().remove("");
        dataset.getDeviceId().remove("");
        dataset.getSelfSignedId().remove("");
        dataset.getIp().remove(null);
        dataset.getDeviceId().remove(null);
        dataset.getSelfSignedId().remove(null);
    }

    private static <T> int removeAll(Iterator<T> to, List<T> from) {
        int removed = 0;
        while (to.hasNext()) {
            if (!from.contains(to.next())) {
                continue;
            }
            ++removed;
            to.remove();
        }

        return removed;
    }
}
