package org.nethergames.observer.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import okhttp3.ConnectionPool;
import okhttp3.Headers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nethergames.microcommon.CommonClient;
import org.nethergames.microcommon.request.RequestResponse;
import org.nethergames.observer.data.evidences.PunishmentEvidence;
import org.nethergames.observer.data.kick.Kick;
import org.nethergames.observer.data.punishment.Punishment;
import org.nethergames.observer.data.punishment.PunishmentReason;
import org.nethergames.observer.data.punishment.PunishmentReasonGrouped;
import org.nethergames.observer.data.punishment.request.PunishmentCreationData;
import org.nethergames.observer.data.punishment.request.PunishmentRequestData;
import org.nethergames.observer.data.punishment.request.PunishmentSearchData;
import org.nethergames.observer.data.punishment.request.PunishmentSearchRequest;
import org.nethergames.observer.data.punishment.request.punishment.BulkPunishment;
import org.nethergames.observer.data.punishment.request.punishment.BulkPunishmentGrouped;
import org.nethergames.observer.data.punishment.request.punishment.PlayerStatus;
import org.nethergames.observer.data.reports.PlayerReportEntry;
import org.nethergames.observer.data.reports.PlayerReportEntry.PlayerReportList;
import org.nethergames.observer.data.reports.PlayerReportEntry.PlayerReportMap;
import org.nethergames.observer.data.reports.request.PlayerReportData;
import org.nethergames.observer.data.reports.request.PlayerReportRequest;
import org.nethergames.observer.data.tracing.AltTracingPushData;
import org.nethergames.observer.data.tracing.request.TracingSearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ObserverClient extends CommonClient {

    public ObserverClient(String apiRoot) {
        super(apiRoot);
    }

    public ObserverClient(String apiRoot, ConnectionPool connectionPool) {
        super(apiRoot, connectionPool);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////// PUNISHMENT ENDPOINTS ///////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RequestResponse<Punishment> createPunishment(@NotNull PunishmentCreationData creationData) throws IOException {
        return runPut("/punishment/player", creationData, new TypeReference<>() {});
    }

    public RequestResponse<List<BulkPunishment>> getPunishmentsRaw(@NotNull List<PunishmentRequestData> data) throws IOException {
        return runPost("/punishment/player", data, new TypeReference<>() {});
    }

    public RequestResponse<List<BulkPunishmentGrouped>> getPunishmentsGrouped(@NotNull List<PunishmentRequestData> data) throws IOException {
        return runPost("/punishment/player?grouped=true", data, new TypeReference<>() {});
    }

    public RequestResponse<List<BulkPunishment>> getPunishmentFor(@NotNull String playerXuid, boolean activeOnly, int depth) throws IOException {
        List<String> allParameters = new ArrayList<>();

        if (depth >= 0) allParameters.add("depth=" + depth);
        if (activeOnly) allParameters.add("activeOnly=true");

        StringBuilder builder = new StringBuilder("?");
        allParameters.forEach(data -> builder.append(data).append("&"));

        return runGet("/punishment/player/" + playerXuid + builder.substring(0, builder.length() - 1), new TypeReference<>() {});
    }

    public RequestResponse<List<BulkPunishmentGrouped>> getPunishmentForGrouped(@NotNull String playerXuid, boolean activeOnly, int depth) throws IOException {
        List<String> allParameters = new ArrayList<>();

        allParameters.add("grouped=true");
        if (depth >= 0) allParameters.add("depth=" + depth);
        if (activeOnly) allParameters.add("activeOnly=true");

        StringBuilder builder = new StringBuilder("?");
        allParameters.forEach(data -> builder.append(data).append("&"));

        return runGet("/punishment/player/" + playerXuid + builder.substring(0, builder.length() - 1), new TypeReference<>() {});
    }

    /**
     * Search the database for punishments using the specified search criteria, starting from the beginning of the
     * result set. The search context may be partial and will be executed as-is.
     * <p>
     * This endpoint will only return a maximum of 50 punishments per request to limit database access. The returned
     * {@link PunishmentSearchData} object includes the list of punishments, as well as the firstOffset and nextOffset values
     * that can be used to retrieve additional punishments. To retrieve the next set of punishments, use the nextOffset
     * value as the offset parameter in a subsequent call to the {@link #searchPunishment(PunishmentSearchRequest, long, boolean)} method.
     *
     * @param searchContext The search criteria for the punishments.
     * @return A {@link PunishmentSearchData} object containing up to 50 punishments, ordered by the date they were issued
     * in descending order, as well as the firstOffset and nextOffset values for retrieving additional punishments.
     * @see #searchPunishment(PunishmentSearchRequest, long, boolean)
     */
    public RequestResponse<PunishmentSearchData> searchPunishment(@NotNull PunishmentSearchRequest searchContext) throws IOException {
        return searchPunishment(searchContext, 0, false);
    }

    /**
     * Search the database for punishments using the specified search criteria. The search context may be partial
     * and will be executed as-is.
     * <p>
     * This endpoint will only return a maximum of 50 punishments per request to limit database access. The returned
     * {@link PunishmentSearchData} object includes the list of punishments, as well as the firstOffset and nextOffset values
     * that can be used to retrieve additional punishments. To retrieve the next set of punishments, use the nextOffset
     * value as the offset parameter in a subsequent call to this method.
     * <p>
     * If the offset parameter is less than or equal to zero, the returned punishments will not use the offset parameter
     * and will start from the beginning of the result set.
     *
     * @param searchContext The search criteria for the punishments.
     * @param offset        The offset for the punishments to be returned. Set to a positive value to retrieve
     *                      punishments starting at the specified offset, or set to 0 or a negative value to retrieve
     *                      punishments starting from the beginning of the result set.
     * @param withEvidence  Whether to include the evidence for each punishment in the response.
     * @return A {@link PunishmentSearchData} object containing up to 50 punishments, ordered by the date they were issued
     * in descending order, as well as the firstOffset and nextOffset values for retrieving additional punishments.
     */
    public RequestResponse<PunishmentSearchData> searchPunishment(@NotNull PunishmentSearchRequest searchContext, long offset, boolean withEvidence) throws IOException {
        return runPost("/punishment/search?offset=" + offset + "&withEvidence=" + withEvidence, searchContext, new TypeReference<>() {});
    }

    /**
     * Returns a {@link Punishment} object for the given punishment "id". If tracedXuid is specified, the method will
     * try to find a link between the punishment "id" and tracedXuid by tracing the xuid stored in the punishment object.
     * By default, the depth for tracing will always be 1.
     *
     * @param id         The unique identifier for the punishment to retrieve.
     * @param tracedXuid The xuid for a player to trace the punishment to (optional).
     * @return A {@link Punishment} object if the given id is found, or null if not found.
     */
    public RequestResponse<Punishment> getPunishmentById(@NotNull String id, String tracedXuid) throws IOException {
        if (tracedXuid != null) {
            return runGet("/punishment/" + id + "?tracedXuid=" + tracedXuid, new TypeReference<>() {});
        } else {
            return runGet("/punishment/" + id, new TypeReference<>() {});
        }
    }

    public RequestResponse<Punishment> setPunishmentNoteById(@NotNull String punishmentId, String note) throws IOException {
        return runPatch("/punishment/" + punishmentId, new AbstractMap.SimpleImmutableEntry<>("note", note), new TypeReference<>() {});
    }

    public RequestResponse<Punishment> deletePunishment(@NotNull String punishmentId, String tracedXuid, String issuer) throws IOException {
        List<String> allParameters = new ArrayList<>();

        if (tracedXuid != null) allParameters.add("tracedXuid=" + tracedXuid);
        if (issuer != null) allParameters.add("issuer=" + issuer);

        StringBuilder builder = new StringBuilder("?");
        allParameters.forEach(data -> builder.append(data).append("&"));

        return runDelete("/punishment/" + punishmentId + builder.substring(0, builder.length() - 1), null, new TypeReference<>() {});
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////// PUNISHMENT REASONS ENDPOINTS ///////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RequestResponse<List<PunishmentReason>> getPunishmentReasons() throws IOException {
        return runGet("/punishment/reasons", new TypeReference<>() {});
    }

    public RequestResponse<PunishmentReasonGrouped> getPunishmentReasonsGrouped() throws IOException {
        return runGet("/punishment/reasons?grouped=true", new TypeReference<>() {});
    }

    public RequestResponse<Void> addPunishmentReason(PunishmentReason reason) throws IOException {
        return runPut("/punishment/reasons", reason, new TypeReference<>() {});
    }

    public RequestResponse<Void> updatePunishmentReason(String name) throws IOException {
        return runDelete("/punishment/reasons/" + name, null, new TypeReference<>() {});
    }

    public RequestResponse<Void> deletePunishmentReason(String name, boolean deletePunishments) throws IOException {
        List<String> allParameters = new ArrayList<>();

        if (deletePunishments) allParameters.add("deletePunishments=true");

        StringBuilder builder = new StringBuilder("?");
        allParameters.forEach(data -> builder.append(data).append("&"));

        return runDelete("/punishment/reasons/" + name + builder.substring(0, builder.length() - 1), null, new TypeReference<>() {});
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////// PLAYER STATUS ENDPOINT //////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RequestResponse<PlayerStatus> getPlayerStatus(String xuid, boolean includePointMappings, int depth) throws IOException {
        List<String> allParameters = new ArrayList<>();

        if (depth >= 0) allParameters.add("depth=" + depth);
        if (includePointMappings) allParameters.add("includePointMaps=true");

        StringBuilder builder = new StringBuilder("?");
        allParameters.forEach(data -> builder.append(data).append("&"));

        return runGet("/player/" + xuid + builder.substring(0, builder.length() - 1), new TypeReference<>() {}); // STATUS
    }

    public RequestResponse<PlayerStatus> getPlayerStatus(AltTracingPushData pushData, boolean includePointMappings, int depth) throws IOException {
        List<String> allParameters = new ArrayList<>();

        if (depth >= 0) allParameters.add("depth=" + depth);
        if (includePointMappings) allParameters.add("includePointMaps=true");

        StringBuilder builder = new StringBuilder("?");
        allParameters.forEach(data -> builder.append(data).append("&"));

        return runPost("/player/" + pushData.getXuid() + builder.substring(0, builder.length() - 1), pushData, new TypeReference<>() {}); // STATUS
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////// TRACING ENDPOINT /////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RequestResponse<List<TracingSearchResult>> getAltAccounts(String xuid) throws IOException {
        return runGet("/player/" + xuid + "/trace", new TypeReference<>() {});
    }

    public RequestResponse<List<TracingSearchResult>> getAltAccounts(String xuid, int depth) throws IOException {
        return runGet("/player/" + xuid + "/trace?depth=" + depth, new TypeReference<>() {});
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////// EVIDENCES ENDPOINT ////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RequestResponse<PunishmentEvidence> uploadEvidence(String punishmentId, PunishmentEvidence evidence) throws IOException {
        return runPut("/punishment/" + punishmentId + "/evidence", evidence, Headers.of("Content-Type", "application/json"), new TypeReference<>() {});
    }

    public RequestResponse<PunishmentEvidence> uploadEvidence(String punishmentId, String uploader, String contentType, InputStream stream) throws IOException {
        return runPut("/punishment/" + punishmentId + "/evidence", stream, Headers.of("Content-Type", contentType, "Upload-Issuer", uploader), new TypeReference<>() {});
    }

    public RequestResponse<List<PunishmentEvidence>> getEvidences(String punishmentId) throws IOException {
        return runGet("/punishment/" + punishmentId + "/evidence", new TypeReference<>() {});
    }

    public RequestResponse<PunishmentEvidence> getEvidence(String punishmentId, @NonNull Long evidenceId) throws IOException {
        return runGet("/punishment/" + punishmentId + "/evidence?evidenceId=" + evidenceId, new TypeReference<>() {});
    }

    public RequestResponse<PunishmentEvidence> updateEvidenceNote(String punishmentId, Long evidenceId, @Nullable String note) throws IOException {
        return runPatch("/punishment/" + punishmentId + "/evidence?evidenceId=" + evidenceId, note, new TypeReference<>() {});
    }

    public RequestResponse<Void> deleteEvidence(String punishmentId, @NonNull Long evidenceId) throws IOException {
        return runDelete("/punishment/" + punishmentId + "/evidence?evidenceId=" + evidenceId, null, new TypeReference<>() {});
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////// REPORTS ENDPOINT /////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public RequestResponse<Void> reportPlayer(PlayerReportData reportData) throws IOException {
        return runPut("/report", reportData, new TypeReference<>() {});
    }

    public RequestResponse<PlayerReportMap> getReportsByPlayers(PlayerReportRequest players) throws IOException {
        return runPost("/report", players, new TypeReference<>() {});
    }

    public RequestResponse<PlayerReportList> getReportsList(int pageLimit) throws IOException {
        return runGet("/report/all-time?pageLimit=" + pageLimit, new TypeReference<>() {});
    }

    public RequestResponse<PlayerReportEntry> getPlayerReports(String xuid) throws IOException {
        return runGet("/report/" + xuid, new TypeReference<>() {});
    }

    public RequestResponse<Void> markReportTraineeClaimed(String xuid, String traineeXuid) throws IOException {
        return runPatch("/report/" + xuid + "/markTraineeClaimed?traineexuid=" + traineeXuid,null, new TypeReference<>() {});
    }

    public RequestResponse<Void> deletePlayerReports(String xuid) throws IOException {
        return runDelete("/report/" + xuid, null, new TypeReference<>() {});
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////// KICK ENDPOINT /////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public RequestResponse<Void> kickPlayer(Kick kick) throws IOException {
        return runPut("/kick", kick, new TypeReference<>() {});
    }
}
