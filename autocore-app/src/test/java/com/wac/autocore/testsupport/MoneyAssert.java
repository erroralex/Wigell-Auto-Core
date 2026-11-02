package com.wac.autocore.testsupport;

import com.wac.autocore.model.value.Money;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * <b>MoneyAssert</b>
 * <p>Responsibility: Compares amounts in tests with {@code compareTo}, never with {@code equals}, and gives
 * a readable failure message with both amounts.</p>
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