package com.wac.autocore.service.discount;

public class NoDiscount implements DiscountStrategy {

    @Override
    public double calculateDiscount(double amount) {
        return 0.0;
    }
}