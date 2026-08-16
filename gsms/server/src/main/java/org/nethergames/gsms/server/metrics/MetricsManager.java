package org.nethergames.gsms.server.metrics;

import com.google.common.base.MoreObjects;
import io.sentry.Sentry;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.nethergames.gsms.data.model.ServerModel;
import org.nethergames.gsms.server.manager.ProxyManager;
import org.nethergames.gsms.server.scheduler.ServerTaskExecutor;
import org.nethergames.gsms.server.server.ServerRegistry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Log4j2(topic = "Metrics")
public class MetricsManager {
    private static final String JDBC_URI = MoreObjects.firstNonNull(System.getenv("JDBC_URI"), "");
    private static final String JDBC_CREDS = MoreObjects.firstNonNull(System.getenv("JDBC_CREDENTIALS"), ":");

    private final ProxyManager proxyManager;
    private final ServerRegistry registry;

    private Connection connection;
    private int matchmakerRequests = 0;
    private int effectiveMatchmakerRuns = 0;

    private final boolean metricsEnabled = System.getenv("INFLUX_ENABLED") != null;

    public MetricsManager(ServerRegistry registry, ProxyManager proxyManager) {
        this.registry = registry;
        this.proxyManager = proxyManager;

        if (!metricsEnabled) return;
        if (JDBC_URI.isEmpty()) {
            log.warn("JDBC_URI environment variable is empty. JDBC session data saving is disabled.");
            return;
        }

        connect();

        ServerTaskExecutor.scheduleRepeating(this::tickQueue, 5, 5, TimeUnit.SECONDS, true);
    }

    public void connect() {
        try {
            Class.forName("com.clickhouse.jdbc.ClickHouseDriver").newInstance();
            if (connection != null) connection.close();
            // username:password format
            String[] creds = JDBC_CREDS.split(":");
            connection = DriverManager.getConnection(JDBC_URI, creds[0], creds[1]);
        } catch (Throwable t) {
            log.error("Error while establishing database connection", t);
        }
    }

    private void tickQueue() {
        try {
            this.pushMetrics();
        } catch (Throwable t) {
            Sentry.captureException(t);

            log.error("Error in Metrics executor", t);
        }
    }

    @SneakyThrows
    public void shutdown() {
        if (!metricsEnabled) return;

        connection.close();
    }

    private void pushMetrics() {
        try (var statement = connection.prepareStatement("SELECT * FROM player_sessions_local LIMIT 1")) {
            statement.executeQuery();
        } catch (Throwable t) {
            log.warn("SQL connection threw " + t.getMessage() + ", reconnecting..");
            connect();
            return;
        }

        var currentTimeframe = Timestamp.from(Instant.now());

        int totalPlayerCount = 0;
        int queueingServers = 0;
        int touchQueueingServers = 0;
        int nonQueueingServers = 0;
        try (var statement = connection.prepareStatement("INSERT INTO game_server_analytics VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (ServerModel model : this.registry.getInternalMap().values()) {
                statement.setTimestamp(1, currentTimeframe);
                statement.setString(2, model.getServerType());
                statement.setString(3, model.getGameType());
                statement.setString(4, model.getRegion());
                statement.setString(5, model.getServerUniqueId());
                statement.setInt(6, model.getPlayerCount());
                statement.setBoolean(7, model.isQueueingState());
                statement.setBoolean(8, model.isTouchOnlyState());
                statement.setFloat(9, model.getLastTps());
                statement.setFloat(10, model.getLastUsage());
                statement.setFloat(11, model.getLastMemoryUsage());
                statement.setFloat(12, model.getMaxPlayerCount());

                statement.addBatch();

                totalPlayerCount += model.getPlayerCount();
                queueingServers += model.isQueueingState() ? 1 : 0;
                touchQueueingServers += model.isTouchOnlyState() ? 1 : 0;
                nonQueueingServers += !model.isQueueingState() && !model.isTouchOnlyState() ? 1 : 0;
            }

            statement.executeBatch();
        } catch (Throwable t) {
            log.error("Error while sending game_server_analytics", t);
        }

        try (var statement = connection.prepareStatement("INSERT INTO network_stats_local VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setTimestamp(1, currentTimeframe);
            statement.setInt(2, totalPlayerCount);
            statement.setInt(3, proxyManager.getCurrentGlobalCount());
            statement.setInt(4, queueingServers);
            statement.setInt(5, touchQueueingServers);
            statement.setInt(6, nonQueueingServers);

            statement.executeUpdate();
        } catch (Throwable t) {
            log.error("Error while sending network_stats_local", t);
        }

        try (var statement = connection.prepareStatement("INSERT INTO gsms_internal_analytics_local VALUES (?, ?, ?, ?)")) {
            statement.setTimestamp(1, currentTimeframe);                // time
            statement.setInt(2, this.matchmakerRequests);               // request_count
            statement.setInt(3, this.effectiveMatchmakerRuns);          // effective_request_ount
            statement.setInt(4, this.registry.getInternalMap().size()); // servers_registered

            statement.executeUpdate();
        } catch (Throwable t) {
            log.error("Error while sending gsms_internal_analytics", t);
        }
    }

    public void increaseMatchmakerRequests() {
        this.matchmakerRequests++;
    }

    public void increaseEffectiveMatchmakerRuns() {
        this.effectiveMatchmakerRuns++;
    }
}

