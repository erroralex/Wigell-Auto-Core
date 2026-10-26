package com.wac.autocore.unit;

import com.wac.autocore.model.AppUser;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * <b>ToStringPersonalDataTest</b>
 * <p>Responsibility: Proves that toString() of person-related entities contains no personal data,
 * so an entity that ends up in a log line or an exception message cannot leak it</p>
 */
class ToStringPersonalDataTest {

    // Distinctive values that cannot appear in a toString() by accident
    private static final String NAME = "Testa Testsson";
    private static final String PHONE = "070-1234567";
    private static final String EMAIL = "testa@example.se";
    private static final String REG_NR = "TST999";
    private static final String FREE_TEXT = "Call Testa Testsson on 070-1234567";

    @Test
    @DisplayName("Customer.toString() contains no name, phone or email")
    void customerToStringContainsNoPersonalData() {
        String text = new Customer(NAME, PHONE, EMAIL).toString();

        assertNoneContained(text, NAME, PHONE, EMAIL);
    }

    @Test
    @DisplayName("Mechanic.toString() contains no name or phone")
    void mechanicToStringContainsNoPersonalData() {
        String text = new Mechanic(NAME, PHONE, "Engine").toString();

        assertNoneContained(text, NAME, PHONE);
    }

    @Test
    @DisplayName("AppUser.toString() contains no username")
    void appUserToStringContainsNoUsername() {
        String text = new AppUser("testa.testsson", "hash", "MECHANIC", true).toString();

        assertNoneContained(text, "testa.testsson", "hash");
    }

    @Test
    @DisplayName("Vehicle.toString() contains no registration number")
    void vehicleToStringContainsNoRegistrationNumber() {
        String text = new Vehicle(REG_NR, "Volvo", "V60", 2020, 1).toString();

        assertNoneContained(text, REG_NR);
    }

    @Test
    @DisplayName("Booking.toString() contains no free-text description")
    void bookingToStringContainsNoDescription() {
        ServiceItem oil = new ServiceItem("Oil change", "", Money.of("899.50"), 45);
        String text = new Booking(1, 2, LocalDate.of(2026, 11, 2), LocalTime.of(8, 0),
                FREE_TEXT, List.of(oil)).toString();

        assertNoneContained(text, FREE_TEXT, NAME, PHONE);
    }

    // Checks each value separately so the failure message names the leaking value
    private static void assertNoneContained(String text, String... personalData) {
        for (String value : personalData) {
            assertFalse(text.contains(value),
                    () -> "toString() leaks personal data '" + value + "': " + text);
        }
    }
}
