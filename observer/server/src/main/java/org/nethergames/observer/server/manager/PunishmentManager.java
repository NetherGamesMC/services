package org.nethergames.observer.server.manager;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import io.micrometer.core.instrument.Metrics;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.joda.time.DateTime;
import org.nethergames.observer.data.general.UsernamePunishmentEntry;
import org.nethergames.observer.data.punishment.*;
import org.nethergames.observer.data.punishment.request.PunishmentCreationData;
import org.nethergames.observer.data.punishment.request.PunishmentRequestData;
import org.nethergames.observer.data.punishment.request.punishment.BulkPunishment;
import org.nethergames.observer.data.punishment.request.punishment.BulkPunishmentGrouped;
import org.nethergames.observer.data.punishment.request.punishment.PlayerStatus;
import org.nethergames.observer.data.punishment.type.PunishmentType;
import org.nethergames.observer.data.reports.ReportResolution;
import org.nethergames.observer.data.tracing.request.TracingSearchEntry;
import org.nethergames.observer.data.tracing.type.TracingType;
import org.nethergames.observer.server.Observer;
import org.nethergames.observer.server.exception.AlreadyPunishedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.*;
import static java.util.stream.Collectors.groupingBy;

@Log4j2(topic = "PunishmentManager")
public class PunishmentManager {

    private final Observer observer;
    private final KeyLockManager lockManager;

    private final Map<String, PunishmentReason> punishmentReasons;

    public PunishmentManager(Observer observer) {
        this.observer = observer;
        this.lockManager = KeyLockManagers.newLock(5, TimeUnit.SECONDS);
        this.punishmentReasons = new HashMap<>();

        observer.getMongoManager().getReasonCollections().find().forEach(o -> punishmentReasons.put(o.getName(), o));
    }

    public PlayerStatus getPlayerStatus(String xuid, int depth, boolean includePointMaps) {
        Punishment activeBan = null;
        Punishment activeMute = null;

        for (var punishment : getPunishments(List.of(new PunishmentRequestData(xuid, depth, true, PunishmentType.values()))).get(0).getPunishments()) {
            var reason = punishment.getReason();
            if (reason.getType().equals(PunishmentType.BAN)) {
                if (activeBan == null || punishment.isPermanent() || punishment.compareTo(activeBan) > 0) {
                    activeBan = punishment;
                }
            } else if ((activeMute == null || punishment.isPermanent() || punishment.compareTo(activeMute) > 0)) {
                activeMute = punishment;
            }
        }

        Map<String, PointMapping> list;
        if (includePointMaps) {
            list = getPointMapping(Set.of(xuid)).get(xuid);
        } else {
            list = null;
        }

        Collection<PlayerComment> comments = Observer.getObserver().getPlayerManager().getCommentsFor(xuid);
        return new PlayerStatus(activeBan, activeMute, list, comments);
    }

    public Map<String, Map<String, PointMapping>> getPointMapping(Set<String> players) {
        var pointPairs = new HashMap<String, Map<String, PointMapping>>();
        var punishments = getPunishments(players, false, PunishmentType.values());

        players.forEach(o -> pointPairs.put(o, new HashMap<>()));

        punishments.forEach((xuid, map) -> {
            var categoryPoint = new HashMap<String, PointMapping>();
            var categoryMap = map.stream().collect(groupingBy(o -> o.getReason().getCategory()));

            categoryMap.forEach((category, punishmentList) -> categoryPoint.put(category, calculatePointMapping(punishmentList).getPointMapping()));

            pointPairs.put(xuid, categoryPoint);
        });

        return pointPairs;
    }

    /**
     * Calculate point mapping of a categorized punishment. Used internally, calculation are based
     * on set of punishments given.
     * <p>
     * This calculations works in the following way:
     * - Sort all punishments in ascending order based on the issuing time
     * - Iterate over each punishment
     * - Add the punishment is permanent, just add the points and do nothing. It's still in effect and won't require any other interaction
     * - If the punishment is not permanent, calculate the time (in months) between this punishment and the last punishment before it in the same category.
     * - Deduct the difference in months from the total points
     * - After all iterations are complete, calculate the difference in months between the last punishment and the current moment.
     * Deduct those points as well. This is to ensure that players points go out of effect after some time
     *
     * @param punishments The list of punishments that are categorized by punishment categories.
     * @return The expiring point pair.
     */
    public PointMappingCalculationResult calculatePointMapping(List<Punishment> punishments) {
        ITransaction transaction = Sentry.startTransaction("point-mapping", "calculate");
        transaction.setDescription("Calculating point mappings");
        transaction.setData("punishments", punishments);


        punishments.sort(Comparator.comparing(Punishment::getValidUntil));

        StringBuilder easyBreakdown = new StringBuilder();

        var expiringPair = new PointMapping();
        int points = 0;
        LocalDate lastPunishmentEndDate = null;
        for (var punishment : punishments) {
            points += punishment.getReason().getPoints();
            easyBreakdown.append("+ " + punishment.getReason().getPoints() + " Point(s): Punishment " + punishment.getId() + "\n");
            if(points > 16) {
                points = Math.min(points, 16);
                easyBreakdown.append("= " + points + " Point(s): Fixed to 16 points\n");
            }

            if (punishment.isPermanent()) {
                continue;
            }

            if (lastPunishmentEndDate != null) {
                LocalDate issuedAt = Instant.ofEpochSecond(punishment.getIssuedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
                long diff = ChronoUnit.MONTHS.between(lastPunishmentEndDate, issuedAt);

                easyBreakdown.append("- " + diff + " Point(s): Time difference between " + lastPunishmentEndDate + " and " + issuedAt + "\n");

                points = (int) Math.max(points - diff, 0);
            }

            lastPunishmentEndDate = Instant.ofEpochSecond(punishment.getValidUntil()).atZone(ZoneId.systemDefault()).toLocalDate();

            expiringPair.setLastPunishment(punishment);
        }

        if (expiringPair.getLastPunishment() != null && !expiringPair.getLastPunishment().isPermanent()) {

            Temporal formattedUntil = Instant.ofEpochSecond(expiringPair.getLastPunishment().getValidUntil()).atZone(ZoneId.systemDefault()).toLocalDate();
            // We also need to reduce points from the last punishment until now, not just between punishments
            if (Instant.ofEpochSecond(expiringPair.getLastPunishment().getValidUntil()).isBefore(Instant.now())) {
                LocalDate now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
                long diff = ChronoUnit.MONTHS.between(formattedUntil, now);


                points = Math.max((int) (points - diff), 0);

                easyBreakdown.append("- " + diff + " Point(s): Last punishment time difference (" + expiringPair.getLastPunishment().getValidUntil() + ")\n");
            }
        }

        expiringPair.setPoints(points);
        if (expiringPair.getLastPunishment() != null) {
            expiringPair.setLastInfraction(expiringPair.getLastPunishment().getIssuedAt());
            expiringPair.setInfractionUntil(expiringPair.getLastPunishment().getValidUntil());
        }

        transaction.setData("result", expiringPair);
        transaction.setData("simpleBreakdown", easyBreakdown.toString());

        System.out.println(" -------- Simple Breakdown -------- \n" + easyBreakdown.toString() + " -------- Simple Breakdown --------");

        transaction.finish(SpanStatus.OK);
        return new PointMappingCalculationResult(expiringPair, easyBreakdown.toString());
    }

    public Punishment createPunishment(PunishmentCreationData data) {
        return lockManager.executeLocked(data.getXuid(), () -> {
            // Check if the reason do exists in database
            var reason = punishmentReasons.get(data.getReason());
            if (reason == null) throw new IllegalArgumentException("There is no punishment reason " + data.getReason());

            // Check if the player is already been punished by this category.
            var existingPunishment = getPunishments(Set.of(data.getXuid()), reason.getType(), reason.getCategory()).get(data.getXuid());
            var pointMapping = calculatePointMapping(existingPunishment).getPointMapping();

            if (pointMapping.isPunishmentActive()) {
                throw new AlreadyPunishedException(pointMapping.getLastPunishment());
            }

            // The punishment logic - insert into database after we have done with this.
            Punishment punishment = new Punishment();
            punishment.setIssuedBy(data.getIssuer());
            punishment.setXuid(data.getXuid());
            punishment.setIssuedAt(Instant.now().getEpochSecond());
            punishment.setReason(reason);
            punishment.setNote(data.getNote());

            pointMapping.addPoints(reason.getPoints());

            DateTime time = pointMapping.calculatePunishmentTime();

            if (time == null) {
                punishment.setPermanent(true);
            } else {
                punishment.setPermanent(false);
                punishment.setValidUntil(Instant.ofEpochMilli(time.toInstant().getMillis()).getEpochSecond());
            }

            MongoManager.insertPunishment(punishment);

            if (reason.getName().equalsIgnoreCase("Inappropriate Username")) {
                MongoManager.addUsernameEntry(new UsernamePunishmentEntry(data.getXuid(), data.getName(), punishment.getId()));
            }

            Metrics.counter("punishments_total").increment();
            Metrics.counter("punishments", "reason", punishment.getReason().getName(), "category", punishment.getReason().getCategory()).increment();

            observer.getReportManager().deleteReports(punishment.getXuid(), Optional.of(ReportResolution.PUNISHED));
            observer.getKafkaManager().broadcastPunishment(punishment);

            return punishment;
        });
    }

    public List<BulkPunishment> getPunishments(List<PunishmentRequestData> requestData) {
        return getPunishments(requestData, false);
    }

    public List<BulkPunishment> getPunishments(List<PunishmentRequestData> requestData, boolean resolveEvidences) {
        List<BulkPunishment> allPunishments = new ArrayList<>();

        requestData.forEach(data -> {
            var groupedPunishment = new BulkPunishment(data.getXuid());

            if (data.getTracingDepth() >= 0) {
                groupedPunishment.setPunishments(getLinkedPunishments(
                        Set.of(data.getXuid()),
                        data.getTracingDepth(),
                        data.isActiveOnly(),
                        data.getPunishmentTypes(),
                        resolveEvidences
                ).get(data.getXuid()));
            } else {
                groupedPunishment.setPunishments(getPunishments(
                        Set.of(data.getXuid()),
                        data.isActiveOnly(),
                        data.getPunishmentTypes(),
                        resolveEvidences
                ).get(data.getXuid()));
            }

            allPunishments.add(groupedPunishment);
        });

        return allPunishments;
    }

    @SneakyThrows
    public List<BulkPunishmentGrouped> getGroupedPunishments(List<PunishmentRequestData> requestData, boolean resolveEvidence) {
        List<BulkPunishmentGrouped> allPunishments = new ArrayList<>();

        requestData.forEach(data -> {
            var groupedPunishment = new BulkPunishmentGrouped();

            if (data.getTracingDepth() >= 0) {
                groupedPunishment.setPunishments(getLinkedPunishments(
                        Set.of(data.getXuid()),
                        data.getTracingDepth(),
                        data.isActiveOnly(),
                        data.getPunishmentTypes(),
                        resolveEvidence
                ).get(data.getXuid()).stream().collect(groupingBy(o -> o.getReason().getCategory())));
            } else {
                groupedPunishment.setPunishments(getPunishments(
                        Set.of(data.getXuid()),
                        data.isActiveOnly(),
                        data.getPunishmentTypes(),
                        resolveEvidence
                ).get(data.getXuid()).stream().collect(groupingBy(o -> o.getReason().getCategory())));
            }

            allPunishments.add(groupedPunishment);
        });

        return allPunishments;
    }

    public Map<String, List<Punishment>> getLinkedPunishments(Set<String> xuids, int depth, boolean onlyActive, PunishmentType[] types) {
        return getLinkedPunishments(xuids, depth, onlyActive, types, false);
    }

    /**
     * Get all linked punishments based on the list of xuid(s), depth of 0 will find the first linked punishments based
     * on the xuid datasets. This method will trace all collections of punishments. The punishments returned will have a
     * special condition, which is, if the xuid is linked to any punishments that were matched by IP, only a non-permanent
     * punishments were returned.
     *
     * @param xuids      A set of xuids to trace the punishment.
     * @param depth      The depth for the tracing punishment, how deep does the tracing must go?
     * @param onlyActive The punishment returned should only be active ones.
     * @param types      The type of the punishment we are looking for.
     * @return The list of punishments mapped by xuid -> { list of punishments }
     */
    @SneakyThrows
    public Map<String, List<Punishment>> getLinkedPunishments(Set<String> xuids, int depth, boolean onlyActive, PunishmentType[] types, boolean resolveEvidence) {
        var punishments = new ConcurrentHashMap<String, List<Punishment>>();

        xuids.forEach(xuid -> {
            var results = observer.getTracingManager().recursiveSearch(new TracingSearchEntry(depth, Set.of(xuid)));
            var didSidList = new HashSet<String>();
            var ipList = new HashSet<String>();

            results.forEach((o, traced) -> traced.forEach(result -> {
                didSidList.addAll(result.getTracingMatches().get(TracingType.DEVICE_ID));
                didSidList.addAll(result.getTracingMatches().get(TracingType.SELF_SIGNED_ID));
                ipList.addAll(result.getTracingMatches().get(TracingType.IP));
            }));

            ipList.removeAll(didSidList);

            var listOfXuids = Stream.of(didSidList, ipList).flatMap(Set::stream).collect(Collectors.toSet());
            var activePunishments = getPunishments(listOfXuids, onlyActive, types).values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(o -> !ipList.contains(o.getXuid()))
                    .filter(o -> !o.getReason().getName().equalsIgnoreCase("Inappropriate Username") || o.getXuid().equalsIgnoreCase(xuid))
                    .collect(Collectors.toList());

            punishments.put(xuid, activePunishments);
        });

        punishments.forEach((key, value) -> {
            value.sort(Comparator.comparing(Punishment::getValidUntil).reversed());

            if (resolveEvidence) {
                value.forEach(o -> o.setEvidences(observer.getEvidenceManager().getEvidences(o.getId())));
            }
        });

        return punishments;
    }

    public Map<String, List<Punishment>> getPunishments(Set<String> xuid, boolean onlyActive, PunishmentType[] punishmentTypes) {
        return getPunishments(xuid, onlyActive, punishmentTypes, false);
    }

    /**
     * Get all active punishments based on all set of xuids, it is determined by the punishment type parameter.
     * Returned objects are a map of xuid -> { set of punishments }
     *
     * @param xuid            The list of xuid to search for.
     * @param onlyActive      The punishment returned should only be active ones.
     * @param punishmentTypes The punishment type to be searched for.
     * @return The map of xuid -> { punishments }
     */
    private Map<String, List<Punishment>> getPunishments(Set<String> xuid, boolean onlyActive, PunishmentType[] punishmentTypes, boolean resolveEvidence) {
        Map<String, List<Punishment>> punishments = new HashMap<>();
        xuid.forEach(o -> punishments.put(o, new ArrayList<>()));

        var filters = Filters.and(
                in("xuid", xuid),
                in("reason.type", punishmentTypes)
        );

        if (onlyActive) {
            filters = Filters.and(
                    filters,
                    or(
                            gt("validUntil", Instant.now().getEpochSecond()),
                            eq("permanent", true)
                    )
            );
        }

        Observer.getObserver().getMongoManager().getPunishmentCollection().find(filters).forEach(punishment -> punishments.get(punishment.getXuid()).add(punishment));
        punishments.forEach((o, v) -> {
            v.sort(Comparator.comparing(Punishment::getValidUntil));

            if (resolveEvidence) {
                v.forEach(d -> d.setEvidences(observer.getEvidenceManager().getEvidences(d.getId())));
            }
        });

        return punishments;
    }

    /**
     * Get all active punishments based on all set of xuids and its category, the punishments returned determined by the
     * punishment type parameter. Returned objects are a map of xuid -> punishment
     *
     * @param xuid            The list of xuid to search for.
     * @param punishmentTypes The punishment type to be searched for.
     * @param category        The category of punishment we are looking for.
     * @return Return a map of xuid -> punishment, the active punishment for the category parameter.
     */
    private Map<String, List<Punishment>> getPunishments(Set<String> xuid, PunishmentType punishmentTypes, String category) {
        Map<String, List<Punishment>> punishments = new HashMap<>();
        xuid.forEach(o -> punishments.put(o, new ArrayList<>()));

        FindIterable<Punishment> queryResults = Observer.getObserver().getMongoManager().getPunishmentCollection().find(Filters.and(
                in("xuid", xuid),
                in("reason.type", punishmentTypes),
                eq("reason.category", category)
        ));

        queryResults.forEach(punishment -> punishments.get(punishment.getXuid()).add(punishment));
        punishments.forEach((o, v) -> v.sort(Comparator.comparing(Punishment::getValidUntil)));

        return punishments;
    }
}
