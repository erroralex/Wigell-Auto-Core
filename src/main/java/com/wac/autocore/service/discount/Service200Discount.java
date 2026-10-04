package com.wac.autocore.service.discount;

public class Service200Discount implements DiscountStrategy {

    @Override
    public double calculateDiscount(double amount) {
        return 200.0;
    }
}