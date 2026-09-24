package com.wac.autocore.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the connection to the local SQLite database and applies the schema defined in schema.sql on startup.
 */
public final class ConnectionManager {

    private static final String DATABASE_URL = "jdbc:sqlite:autocore.db";
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    private static Connection connection;

    private ConnectionManager() {}

    /**
     * Returns a shared, open connection to the local SQLite database.
     * Foreign key enforcement is enabled on the connection.
     */
    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DATABASE_URL);

                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");
                }

            }
            return connection;

        } catch (SQLException e) {
            throw new IllegalStateException("Could not connect to SQLite database: " + DATABASE_URL, e);
        }

    }

    /**
     * Reads schema.sql from the classpath and executes every statement against the database using CREATE TABLE IF NOT EXISTS,
     * so this can be called on every application startup without deleting existing data.
     */
    public static synchronized void initializeSchema() {
        String schema = readSchemaResource();

        try (Statement statement = getConnection().createStatement()) {
            for (String sql : schema.split(";")) {
                String trimmed = sql.trim();

                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize database schema", e);
        }
    }

    private static String readSchemaResource() {
        try (InputStream in = ConnectionManager.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Schema resource not found: " + SCHEMA_RESOURCE);
            }

            StringBuilder builder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }

            }
            return builder.toString();

        } catch (IOException e) {
            throw new UncheckedIOException("Could not read schema resource: " + SCHEMA_RESOURCE, e);
        }
    }

}