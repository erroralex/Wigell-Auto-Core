package com.wac.autocore.unit;

import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>MoneyTest</b>
 * <p>Ansvar: Bevisar att belopp är exakta till öret och avrundas likadant överallt:
 * två decimaler, HALF_UP, och att komma och punkt tolkas lika.</p>
 */
class MoneyTest {

    @Test
    @DisplayName("0.1 + 0.2 is exactly 0.30")
    void addingPointOneAndPointTwoGivesPointThirty() {
        Money sum = Money.of(0.1).add(Money.of(0.2));

        assertMoney(Money.of("0.30"), sum);
        assertEquals("0.30", sum.toString());
    }

    @Test
    @DisplayName("A double with floating-point error is rounded to the öre")
    void doubleWithFloatingPointErrorIsRoundedToTwoDecimals() {
        assertEquals("0.30", Money.of(0.1 + 0.2).toString());
    }

    @ParameterizedTest(name = "{0} rounds to {1}")
    @CsvSource({
            "0.125, 0.13",
            "0.124, 0.12",
            "0.135, 0.14",
            "0.005, 0.01",
            "0.004, 0.00",
            "2.675, 2.68",
            "-0.125, -0.13",
            "-0.124, -0.12",
            "899, 899.00",
            "899.9, 899.90"
    })
    void roundsToTwoDecimalsHalfUp(String input, String expected) {
        assertEquals(expected, Money.of(new BigDecimal(input)).toString());
    }

    @Test
    void scaleIsAlwaysTwoDecimals() {
        assertEquals(2, Money.ZERO.toBigDecimal().scale());
        assertEquals(2, Money.of("5").toBigDecimal().scale());
        assertEquals(2, Money.of("1E+3").toBigDecimal().scale());
        assertEquals("1000.00", Money.of("1E+3").toString());
    }

    @ParameterizedTest(name = "\"{0}\" is parsed as {1}")
    @CsvSource({
            "'12,5', 12.50",
            "'12.5', 12.50",
            "' 12,50 ', 12.50",
            "'1097,499', 1097.50",
            "'-5,25', -5.25"
    })
    void ofStringAcceptsCommaAndPoint(String text, String expected) {
        assertMoney(Money.of(expected), Money.of(text));
    }

    @Test
    void ofStringThrows_WhenTextIsNotANumber() {
        assertThrows(NumberFormatException.class, () -> Money.of("abc"));
        assertThrows(NumberFormatException.class, () -> Money.of(""));
    }

    @Test
    void ofThrows_WhenValueIsNull() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null));
        assertThrows(NullPointerException.class, () -> Money.of((String) null));
    }

    @Test
    void sumOfEmptyCollectionIsZero() {
        assertMoney(Money.ZERO, Money.sum(Collections.<Money>emptyList()));
    }

    @Test
    void sumAddsExactly() {
        List<Money> tenTimesTenOre = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenTimesTenOre.add(Money.of("0.10"));
        }

        assertMoney(Money.of("1.00"), Money.sum(tenTimesTenOre));
        assertMoney(Money.of("1299.00"), Money.sum(Arrays.asList(Money.of(899), Money.of(400))));
    }

    @Test
    void sumThrows_WhenCollectionContainsNull() {
        assertThrows(NullPointerException.class,
                () -> Money.sum(Arrays.asList(Money.of(1), null)));
    }

    @Test
    void subtractAndAddAreExact() {
        assertMoney(Money.of("0.10"), Money.of("0.30").subtract(Money.of("0.20")));
        assertMoney(Money.of("-0.10"), Money.of("0.20").subtract(Money.of("0.30")));
    }

    @Test
    void multiplyRoundsHalfUp() {
        assertMoney(Money.of("89.90"), Money.of(899).multiply(new BigDecimal("0.10")));
        assertMoney(Money.of("0.08"), Money.of("0.15").multiply(new BigDecimal("0.5")));
    }

    @Test
    void minReturnsTheSmallerAmount() {
        assertMoney(Money.of(200), Money.of(200).min(Money.of(899)));
        assertMoney(Money.of(50), Money.of(899).min(Money.of(50)));
    }

    @Test
    void signChecks() {
        assertTrue(Money.of("-0.01").isNegative());
        assertTrue(Money.ZERO.isZero());
        assertTrue(Money.of("0.004").isZero());
        assertTrue(Money.of("0.01").isPositive());
        assertFalse(Money.ZERO.isNegative());
        assertFalse(Money.ZERO.isPositive());
    }

    @Test
    void compareToIgnoresHowTheAmountWasWritten() {
        assertEquals(0, Money.of("1.5").compareTo(Money.of("1.50")));
        assertEquals(0, Money.of("1.50").compareTo(Money.of(1.5)));
        assertTrue(Money.of("1.49").compareTo(Money.of("1.50")) < 0);
        assertTrue(Money.of("1.51").compareTo(Money.of("1.50")) > 0);
    }

    @Test
    void equalAmountsHaveEqualHashCode() {
        assertEquals(Money.of("1.5").hashCode(), Money.of("1.50").hashCode());
    }

    @Test
    void toDoubleGivesTheStoredValueForTheDatabase() {
        assertEquals(1097.5, Money.of("1097.50").toDouble(), 0.0);
    }
}
