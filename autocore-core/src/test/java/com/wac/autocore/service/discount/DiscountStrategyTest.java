package com.wac.autocore.service.discount;

import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;

/**
 * <b>DiscountStrategyTest</b>
 * <p>Responsibility: Locks down the rounding rule. Each strategy rounds its discount to the öre with HALF_UP.</p>
 */
class DiscountStrategyTest {

    @ParameterizedTest(name = "10 % of {0} is {1}")
    @CsvSource({
            "899, 89.90",
            "1097.50, 109.75",
            "0.05, 0.01",
            "1.05, 0.11",
            "99.95, 10.00",
            "0.04, 0.00"
    })
    void tenPercentIsRoundedToOreHalfUp(String amount, String expected) {
        assertMoney(Money.of(expected), new Welcome10Discount().calculateDiscount(Money.of(amount)));
        assertMoney(Money.of(expected), new VipDiscount().calculateDiscount(Money.of(amount)));
    }

    @Test
    void noDiscountIsZero() {
        assertMoney(Money.ZERO, new NoDiscount().calculateDiscount(Money.of(899)));
    }

    @Test
    void service200IsTwoHundredRegardlessOfAmount() {
        assertMoney(Money.of(200), new Service200Discount().calculateDiscount(Money.of(50)));
        assertMoney(Money.of(200), new Service200Discount().calculateDiscount(Money.of(899)));
    }
}
