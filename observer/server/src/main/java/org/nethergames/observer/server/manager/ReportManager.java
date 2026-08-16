package org.nethergames.observer.server.manager;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.Sorts;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.log4j.Log4j2;
import org.nethergames.observer.data.general.PlayerMessage;
import org.nethergames.observer.data.reports.PlayerReportEntry;
import org.nethergames.observer.data.reports.ReportResolution;
import org.nethergames.observer.data.reports.ServerReportBroadcast;
import org.nethergames.observer.data.reports.request.PlayerReportData;
import org.nethergames.observer.server.Observer;
import org.nethergames.social.rpc.GetPlayerStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;
import static org.nethergames.observer.data.reports.PlayerReportEntry.PlayerReportList;
import static org.nethergames.observer.data.reports.PlayerReportEntry.PlayerReportMap;

@Log4j2(topic = "ReportManager")
public class ReportManager {
    private final Observer observer;
    private final MongoCollection<PlayerReportEntry> reportCollections;

    public ReportManager(Observer observer) {
        this.observer = observer;
        this.reportCollections = observer.getMongoManager().getReportsCollections();
    }

    /**
     * Add a report to the player list. The report will put the data such that the class for report entry
     * contains player whose reported this player, the replay id mapping.
     *
     * @param report The report data that was sent by the requester.
     * @return {@code true} if the report was successfully added.
     */
    public boolean addReport(PlayerReportData report) {
        var reportEntry = reportCollections.find(eq("player", report.getPlayer())).first();
        if (reportEntry == null) {
            reportEntry = new PlayerReportEntry(report.getPlayer());
        }

        if (!reportEntry.getPlayersReported().add(report.getReporter())) {
            return false;
        }

        reportEntry.addReport(report);

        reportCollections.findOneAndReplace(eq("player", report.getPlayer()), reportEntry, (new FindOneAndReplaceOptions()).upsert(true));
        observer.getKafkaManager().broadcastReports(new ServerReportBroadcast(reportEntry, report));

        Metrics.counter("observer_reports_total").increment();
        Metrics.counter("observer_reports", "reason", report.getReportReason()).increment();

        return true;
    }

    /**
     * Retrieve all reports by the list of players given.
     *
     * @param players The list of player xuid(s)
     * @return The player report mapping of xuid -> report entry
     */
    public PlayerReportMap getReportsBulk(List<String> players) {
        PlayerReportMap bulkList = new PlayerReportMap();
        players.forEach(player -> bulkList.put(player, getReportsFor(player)));

        return bulkList;
    }

    /**
     * Returns all reports sorted by the recommended algorithm, the most reported players and the
     * most recent reported players will always stay on top of the report entry list, this should help
     * staff to know which player would need the most look.
     *
     * @return A list of ReportEntry
     */
    public PlayerReportList getReportsRecommended(int limit) {
        var request = reportCollections.find(not(exists("traineeClaimed"))).sort(
                Sorts.orderBy(
                        Sorts.descending("totalReports"),
                        Sorts.descending("lastReported")
                )
        ).limit(100);


        var list = new PlayerReportList();
        request.forEach(list::add);

        List<String> names = list.stream().map(PlayerReportEntry::getPlayer).toList();

        Map<String, Boolean> onlineStatus = Observer.getObserver().getSocialManager().arePlayersOnline(names);

        log.info("Debug info: list: {}, onlineStatus: {}, names: {}", list.size(), onlineStatus.size(), names.size());

        list.sort((a, b) -> {
            boolean aOnline = onlineStatus.get(a.getPlayer());
            boolean bOnline = onlineStatus.get(b.getPlayer());

            if (aOnline == bOnline) {
                if (a.getTotalReports() == b.getTotalReports()) {
                    return a.getLastReported().after(b.getLastReported()) ? -1 : 1;
                }

                return a.getTotalReports() >= b.getTotalReports() ? -1 : 1;
            }

            if (aOnline) {
                return -1;
            } else {
                return 1;
            }
        });


        if (limit > 0) {
            list.subList(0, Math.min(list.size(), limit));
        }

        return list;
    }

    /**
     * Return the reports for the given xuid. It will return existing reports for the given xuid.
     * If the report for the given xuid do not exist in the list, it will then provide the default
     * report entry.
     *
     * @param player The player xuid
     * @return The report entry.
     */
    public PlayerReportEntry getReportsFor(String player) {
        return reportCollections.find(eq("player", player)).first();
    }


    /**
     * Marking the report of this given player, if existing, as claimed by trainee to no longer be listed
     *
     * @param player  The player xuid
     * @param trainee The trainee claiming this report
     * @return {@code true} if the report was modified.
     */

    public boolean traineeClaimReport(String player, String trainee) {
        return reportCollections.updateOne(eq("player", player), set("traineeClaimed", trainee)).getModifiedCount() > 0;
    }


    /**
     * Delete all reports that was reported to this player.
     *
     * @param player     The player itself.
     * @param resolution Whether the report was successful or not
     * @return {@code true} if the report was found.
     */
    public boolean deleteReports(String player, Optional<ReportResolution> resolution) {
        if (resolution.isPresent()) {
            PlayerReportEntry report = getReportsFor(player);
            if (report == null) return false;
            PlayerMessage message = null;

            Map<String, String> nameMappings = Observer.getObserver().getApiManager().getXboxNameMappings(Collections.singletonList(player));

            switch (resolution.get()) {
                case PUNISHED ->
                        message = PlayerMessage.Static("§cObserver §r§l»§r §aYour recent report for §c§l" + nameMappings.getOrDefault(player, "Unknown player") + "§r§a has been reviewed by a staff member and the player is now punished. Thank you for helping keep NetherGames more enjoyable for everyone!");
                case INSUFFICIENT ->
                        message = PlayerMessage.Static("§cObserver §r§l»§r §aA staff member has reviewed your report for §c§l" + nameMappings.getOrDefault(player, "Unknown player") + "§r§a and was not able to find sufficient evidence to punish the player. If you possess some evidence to prove the player was breaking the server rules, please report them at §chttps://ngmc.co/request");
            }

            for (String reportingPlayer : report.getPlayersReported()) {
                Observer.getObserver().getSocialManager().sendPlayerMessage(reportingPlayer, message);
            }
        }
        return reportCollections.deleteOne(eq("player", player)).getDeletedCount() > 0;
    }
}
