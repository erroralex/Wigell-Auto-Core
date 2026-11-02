package com.wac.autocore.service.discount;

import com.wac.autocore.model.value.Money;

public class NoDiscount implements DiscountStrategy {

    @Override
    public Money calculateDiscount(Money amount) {
        return Money.ZERO;
    }
}