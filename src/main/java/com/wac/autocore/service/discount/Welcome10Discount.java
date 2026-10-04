package com.wac.autocore.service.discount;

public class Welcome10Discount implements DiscountStrategy {

    @Override
    public double calculateDiscount(double amount) {
        return amount * 0.10;
    }
}