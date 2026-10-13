package com.wac.autocore.service.discount;

import com.wac.autocore.model.value.Money;

public class Service200Discount implements DiscountStrategy {

    @Override
    public Money calculateDiscount(Money amount) {
        return Money.of(200.0);
    }
}