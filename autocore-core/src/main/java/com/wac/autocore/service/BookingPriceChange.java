package com.wac.autocore.service;

import com.wac.autocore.model.value.Money;

// Holds information about which prices have changed in the new booking compared to the old one.
// Used to show a summary of price changes in the booking dialog.
public final class BookingPriceChange {

    private final int serviceItemId;
    private final String serviceName;
    private final Money previousPrice;
    private final Money currentPrice;

    public BookingPriceChange(int serviceItemId,
                              String serviceName,
                              Money previousPrice,
                              Money currentPrice) {
        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.previousPrice = previousPrice;
        this.currentPrice = currentPrice;
    }

    public int getServiceItemId() {
        return serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Money getPreviousPrice() {
        return previousPrice;
    }

    public Money getCurrentPrice() {
        return currentPrice;
    }

}
