package com.wac.autocore.service.discount;

import com.wac.autocore.model.value.Money;

public interface DiscountStrategy {
    Money calculateDiscount(Money amount);
}