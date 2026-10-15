package com.wac.autocore.testsupport;

import com.wac.autocore.model.value.Money;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>MoneyAssert</b>
 * <p>Ansvar: Jämför belopp i tester med {@code compareTo}, aldrig med {@code equals}, och ger
 * ett läsbart felmeddelande med båda beloppen.</p>
 */
public final class MoneyAssert {

    private MoneyAssert() {
    }

    public static void assertMoney(Money expected, Money actual) {
        assertMoney(expected, actual, null);
    }

    public static void assertMoney(Money expected, Money actual, String message) {
        String prefix = message == null ? "" : message + " ==> ";

        if (expected == null || actual == null || expected.compareTo(actual) != 0) {
            fail(prefix + "expected: <" + expected + "> but was: <" + actual + ">");
        }
    }
}
