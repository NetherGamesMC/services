package org.nethergames.observer.server.manager;

import io.sentry.Sentry;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.DriverManager;

@Log4j2(topic="DatabaseManager")
public class DatabaseManager {
    @Getter
    private Connection connection;

    public DatabaseManager() {
        if (System.getenv("JDBC_URI") == null) {
            log.warn("JDBC_URI environment variable is empty. JDBC is disabled.");
            return;
        }
        connect();
    }

    public void shutdown() {
        try {
            if (connection != null) connection.close();
        } catch (Throwable t) {
            Sentry.captureException(t);
            log.error("Error while closing sql connection", t);
        }
    }

    public void connect() {
        try {
            Class.forName("org.mariadb.jdbc.Driver").newInstance();
            if (connection != null) connection.close();
            // username:password format
            String[] creds = System.getenv("JDBC_CREDS").split(":");
            connection = DriverManager.getConnection(System.getenv("JDBC_URI"), creds[0], creds[1]);
        } catch (Throwable t) {
            log.error("Error while establishing database connection", t);
        }
    }
}