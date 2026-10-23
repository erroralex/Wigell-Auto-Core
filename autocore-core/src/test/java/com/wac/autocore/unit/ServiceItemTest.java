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
 * <p>Responsibility: Proves that the price rule is owned by {@link ServiceItem}: the price must be greater than 0,
 * both when the service is created and when the price is changed.</p>
 */
class ServiceItemTest {

    private static final String INVALID_PRICE = "error.serviceItem.invalidPrice";

    @Test
    @DisplayName("The constructor rejects price 0")
    void constructorRejectsZeroPrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new ServiceItem("Oil change", "description", Money.ZERO, 45));
        assertEquals(INVALID_PRICE, e.getMessageKey());
    }

    @Test
    @DisplayName("The constructor rejects a negative price")
    void constructorRejectsNegativePrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new ServiceItem("Oil change", "description", Money.of(-1), 45));
        assertEquals(INVALID_PRICE, e.getMessageKey());
    }

    @Test
    @DisplayName("The constructor rejects a missing price")
    void constructorRejectsNullPrice() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> new ServiceItem("Oil change", "description", null, 45));
        assertEquals(INVALID_PRICE, e.getMessageKey());
    }

    @Test
    @DisplayName("The constructor keeps a valid price")
    void constructorKeepsValidPrice() {
        ServiceItem item = new ServiceItem("Oil change", "description", Money.of("899.50"), 45);

        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("changePrice rejects price 0 and leaves the price unchanged")
    void changePriceRejectsZeroAndKeepsOldPrice() {
        ServiceItem item = new ServiceItem("Oil change", "description", Money.of("899.50"), 45);

        ValidationException e = assertThrows(ValidationException.class,
                () -> item.changePrice(Money.ZERO));

        assertEquals(INVALID_PRICE, e.getMessageKey());
        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("changePrice rejects a missing price and leaves the price unchanged")
    void changePriceRejectsNullAndKeepsOldPrice() {
        ServiceItem item = new ServiceItem("Oil change", "description", Money.of("899.50"), 45);

        ValidationException e = assertThrows(ValidationException.class,
                () -> item.changePrice(null));

        assertEquals(INVALID_PRICE, e.getMessageKey());
        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("changePrice sets a valid new price")
    void changePriceAcceptsValidPrice() {
        ServiceItem item = new ServiceItem("Oil change", "description", Money.of("899.50"), 45);

        item.changePrice(Money.of("999.00"));

        assertMoney(Money.of("999.00"), item.getPrice());
    }
}
