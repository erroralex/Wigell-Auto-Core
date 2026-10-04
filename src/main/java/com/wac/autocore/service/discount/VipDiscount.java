package com.wac.autocore.service.discount;

public class VipDiscount implements DiscountStrategy {

    @Override
    public double calculateDiscount(double amount) {
        return amount * 0.10;
    }
}