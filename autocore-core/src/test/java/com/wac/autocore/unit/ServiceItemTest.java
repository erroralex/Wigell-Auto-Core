package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <b>ServiceItemTest</b>
 * <p>Ansvar: Bevisar att prisregeln ägs av {@link ServiceItem}: priset måste vara större än 0,
 * både när tjänsten skapas och när priset ändras.</p>
 */
class ServiceItemTest {

    private static final String INVALID_PRICE = "error.serviceItem.invalidPrice";

    @Test
    @DisplayName("Konstruktorn avvisar pris 0")
    void constructorRejectsZeroPrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new ServiceItem("Oljebyte", "beskrivning", Money.ZERO, 45));
        assertEquals(INVALID_PRICE, e.getMessageKey());
    }

    @Test
    @DisplayName("Konstruktorn avvisar negativt pris")
    void constructorRejectsNegativePrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new ServiceItem("Oljebyte", "beskrivning", Money.of(-1), 45));
        assertEquals(INVALID_PRICE, e.getMessageKey());
    }

    @Test
    @DisplayName("Konstruktorn avvisar saknat pris")
    void constructorRejectsNullPrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new ServiceItem("Oljebyte", "beskrivning", null, 45));
        assertEquals(INVALID_PRICE, e.getMessageKey());
    }

    @Test
    @DisplayName("Konstruktorn behåller ett giltigt pris")
    void constructorKeepsValidPrice() {
        ServiceItem item = new ServiceItem("Oljebyte", "beskrivning", Money.of("899.50"), 45);

        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("changePrice avvisar pris 0 och lämnar priset oförändrat")
    void changePriceRejectsZeroAndKeepsOldPrice() {
        ServiceItem item = new ServiceItem("Oljebyte", "beskrivning", Money.of("899.50"), 45);

        ValidationException e = assertThrows(ValidationException.class,
                () -> item.changePrice(Money.ZERO));

        assertEquals(INVALID_PRICE, e.getMessageKey());
        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("changePrice avvisar saknat pris och lämnar priset oförändrat")
    void changePriceRejectsNullAndKeepsOldPrice() {
        ServiceItem item = new ServiceItem("Oljebyte", "beskrivning", Money.of("899.50"), 45);

        ValidationException e = assertThrows(ValidationException.class,
                () -> item.changePrice(null));

        assertEquals(INVALID_PRICE, e.getMessageKey());
        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("changePrice sätter ett giltigt nytt pris")
    void changePriceAcceptsValidPrice() {
        ServiceItem item = new ServiceItem("Oljebyte", "beskrivning", Money.of("899.50"), 45);

        item.changePrice(Money.of("999.00"));

        assertMoney(Money.of("999.00"), item.getPrice());
    }
}
