package com.wac.autocore.service.discount;

import com.wac.autocore.model.value.Money;

import java.math.BigDecimal;

public class VipDiscount implements DiscountStrategy {

    @Override
    public Money calculateDiscount(Money amount) {
        return amount.multiply(new BigDecimal("0.10"));
    }
}