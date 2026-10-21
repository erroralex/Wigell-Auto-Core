package com.wac.autocore.service;

import com.wac.autocore.model.value.Money;

// Returnerar information om prisändringar om vilka priser som ändrats i den nya bokningen jämfört med den gamla.
// Används för att visa en sammanställning av prisändringar i bokningsdialogen.
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
