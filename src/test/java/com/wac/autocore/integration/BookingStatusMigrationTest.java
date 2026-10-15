package com.wac.autocore.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <b>BookingStatusMigrationTest</b>
 * <p>Ansvar: Bevisar att V11 städar läckta statusvärden på {@code booking}. Testet migrerar
 * först till V10, skriver in rader med status {@code CONFIRMED} och {@code DRAFT}, och kör
 * sedan migrationerna till senaste versionen. Efteråt ska de läckta raderna ha status
 * {@code WORK_ORDER_CREATED}, medan en giltig rad är orörd.</p>
 */
class BookingStatusMigrationTest {

    @TempDir
    static Path tempDir;

    @Test
    @DisplayName("V11 rewrites leaked CONFIRMED and DRAFT statuses to WORK_ORDER_CREATED")
    void v11RewritesLeakedStatuses() throws SQLException {
        String url = "jdbc:sqlite:" + tempDir.resolve("booking-status-migration.db").toAbsolutePath();

        // Migrera till V10, så V11 inte har körts än
        Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:db/migration")
                .target("10")
                .load()
                .migrate();

        // Skriv in läckta statusar direkt i tabellen
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "INSERT INTO customer (name, phone, email, vip) VALUES ('Test', '070', 't@e.se', 0)");
            statement.executeUpdate(
                    "INSERT INTO mechanic (name, phone, specialization) VALUES ('Test', '070', 'General')");
            statement.executeUpdate(
                    "INSERT INTO vehicle (registration_number, brand, model, year, customer_id) " +
                            "VALUES ('TST001', 'Volvo', 'V60', 2020, 1)");

            statement.executeUpdate(
                    "INSERT INTO booking (vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                            "VALUES (1, 1, '2026-11-03', '08:00', '09:00', 'Leaked CONFIRMED', 'CONFIRMED')");
            statement.executeUpdate(
                    "INSERT INTO booking (vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                            "VALUES (1, 1, '2026-11-04', '08:00', '09:00', 'Leaked DRAFT', 'DRAFT')");
            statement.executeUpdate(
                    "INSERT INTO booking (vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                            "VALUES (1, 1, '2026-11-05', '08:00', '09:00', 'Already valid', 'BOOKED')");
        }

        // Kör resten, inklusive V11
        Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        // Verifiera att de läckta värdena har skrivits om, och att giltig rad är orörd
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            assertEquals("WORK_ORDER_CREATED", statusOf(statement, "Leaked CONFIRMED"),
                    "CONFIRMED should be rewritten to WORK_ORDER_CREATED");
            assertEquals("WORK_ORDER_CREATED", statusOf(statement, "Leaked DRAFT"),
                    "DRAFT should be rewritten to WORK_ORDER_CREATED");
            assertEquals("BOOKED", statusOf(statement, "Already valid"),
                    "BOOKED should be untouched");
        }
    }

    private String statusOf(Statement statement, String description) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT status FROM booking WHERE description = '" + description + "'")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            throw new AssertionError("No booking with description: " + description);
        }
    }
}