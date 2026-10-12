package com.wac.autocore.service;

// Returnerar information om prisändringar om vilka priser som ändrats i den nya bokningen jämfört med den gamla.
// Används för att visa en sammanställning av prisändringar i bokningsdialogen.
public final class BookingPriceChange {

    private final int serviceItemId;
    private final String serviceName;
    private final double previousPrice;
    private final double currentPrice;

    public BookingPriceChange(int serviceItemId,
                              String serviceName,
                              double previousPrice,
                              double currentPrice) {
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

    public double getPreviousPrice() {
        return previousPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

}
