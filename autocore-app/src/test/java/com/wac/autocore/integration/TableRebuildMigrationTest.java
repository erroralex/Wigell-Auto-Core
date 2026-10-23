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

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>TableRebuildMigrationTest</b>
 * <p>Responsibility: Proves that V7 and V13 run outside Flyway's transaction, as their
 * {@code .sql.conf} files specify. Both scripts rebuild a parent table and turn off
 * the FK check with {@code PRAGMA foreign_keys = OFF}. SQLite ignores that PRAGMA inside
 * a transaction, so if Flyway does not read the {@code .conf} file the FK check stops
 * {@code DROP TABLE} and the migration fails.</p>
 *
 * <p>The connection is opened with {@code foreign_keys=true}, just like the app's Hikari pool.
 * Without it the FK check is disabled in SQLite and the test would pass even if
 * the {@code .conf} file were ignored.</p>
 *
 * <p>The test first migrates to the version before the rebuild, inserts child rows that point
 * to the table being rebuilt, and then runs the rest. Afterwards the rows should still exist with
 * the same links and {@code PRAGMA foreign_key_check} should be empty.</p>
 */
class TableRebuildMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("V7 rebuilds work_order without losing invoices that point to it")
    void v7KeepsInvoicesLinkedToWorkOrder() throws SQLException {
        String url = urlFor("v7-rebuild.db");

        migrate(url, "6");

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "INSERT INTO customer (id, name, phone, email, vip) VALUES (9001, 'Mig', '070', 'm@e.se', 0)");
            statement.executeUpdate(
                    "INSERT INTO vehicle (id, registration_number, brand, model, year, customer_id) " +
                            "VALUES (9001, 'MIG001', 'Volvo', 'V60', 2020, 9001)");
            statement.executeUpdate(
                    "INSERT INTO mechanic (id, name, phone, specialization) VALUES (9001, 'Mig', '070', 'General')");
            statement.executeUpdate(
                    "INSERT INTO booking (id, vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                            "VALUES (9001, 9001, 9001, '2026-11-03', '08:00', '09:00', 'V7 migration', 'BOOKED')");
            statement.executeUpdate(
                    "INSERT INTO work_order (id, booking_id, mechanic_id, status) VALUES (9001, 9001, 9001, 'CREATED')");
            statement.executeUpdate(
                    "INSERT INTO invoice (id, work_order_id, invoice_date, amount, discount, total_amount, paid) " +
                            "VALUES (9001, 9001, '2026-11-03', 500, 0, 500, 0)");
        }

        migrate(url, null);

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            assertEquals(9001, singleInt(statement, "SELECT work_order_id FROM invoice WHERE id = 9001"),
                    "Invoice should still point to the same work order");

            try (ResultSet rs = statement.executeQuery(
                    "SELECT type, status, vehicle_id FROM work_order WHERE id = 9001")) {
                assertTrue(rs.next(), "Work order 9001 should survive the rebuild");
                assertEquals("PLANNED", rs.getString("type"), "Existing work orders become PLANNED");
                assertEquals("CONFIRMED", rs.getString("status"), "CREATED should be rewritten to CONFIRMED");
                assertEquals(9001, rs.getInt("vehicle_id"), "Vehicle should be copied from the booking");
            }

            assertNoForeignKeyViolations(statement);
        }
    }

    @Test
    @DisplayName("V13 rebuilds booking without losing rows that point to it")
    void v13KeepsChildRowsLinkedToBooking() throws SQLException {
        String url = urlFor("v13-rebuild.db");

        migrate(url, "12");

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "INSERT INTO customer (id, name, phone, email, vip) VALUES (9002, 'Mig', '070', 'm@e.se', 0)");
            statement.executeUpdate(
                    "INSERT INTO vehicle (id, registration_number, brand, model, year, customer_id) " +
                            "VALUES (9002, 'MIG002', 'Volvo', 'V60', 2020, 9002)");
            statement.executeUpdate(
                    "INSERT INTO mechanic (id, name, phone, specialization) VALUES (9002, 'Mig', '070', 'General')");
            statement.executeUpdate(
                    "INSERT INTO service_item (id, name, description, price, estimated_minutes) " +
                            "VALUES (9002, 'Mig service', 'Test', 500, 60)");
            statement.executeUpdate(
                    "INSERT INTO booking (id, vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                            "VALUES (9002, 9002, 9002, '2026-11-03', '08:00', '09:00', 'V13 migration', 'BOOKED')");
            statement.executeUpdate(
                    "INSERT INTO booking_service_item (booking_id, service_item_id, service_name, price_at_booking, duration_minutes) " +
                            "VALUES (9002, 9002, 'Mig service', 500, 60)");
            statement.executeUpdate(
                    "INSERT INTO work_order (id, booking_id, mechanic_id, vehicle_id, type, status) " +
                            "VALUES (9002, 9002, 9002, 9002, 'PLANNED', 'CONFIRMED')");
        }

        migrate(url, null);

        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            assertEquals(9002, singleInt(statement, "SELECT mechanic_id FROM booking WHERE id = 9002"),
                    "Booking should keep its mechanic");
            assertEquals(1, singleInt(statement, "SELECT COUNT(*) FROM booking_service_item WHERE booking_id = 9002"),
                    "Booked service item should still point to the booking");
            assertEquals(9002, singleInt(statement, "SELECT booking_id FROM work_order WHERE id = 9002"),
                    "Work order should still point to the booking");

            assertNoForeignKeyViolations(statement);
        }
    }

    private String urlFor(String fileName) {
        return "jdbc:sqlite:" + tempDir.resolve(fileName).toAbsolutePath() + "?foreign_keys=true";
    }

    /**
     * Migrates to {@code target}, or to the latest version if target is null.
     */
    private void migrate(String url, String target) {
        var config = Flyway.configure()
                .dataSource(url, null, null)
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        config.load().migrate();
    }

    private int singleInt(Statement statement, String sql) throws SQLException {
        try (ResultSet rs = statement.executeQuery(sql)) {
            assertTrue(rs.next(), "Query returned no rows: " + sql);
            return rs.getInt(1);
        }
    }

    private void assertNoForeignKeyViolations(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("PRAGMA foreign_key_check")) {
            assertFalse(rs.next(), "PRAGMA foreign_key_check should report no violations");
        }
    }
}
