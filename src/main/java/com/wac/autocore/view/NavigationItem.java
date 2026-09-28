package com.wac.autocore.view;

/**
 * <b>NavigationItem</b>
 * <p>Ansvar: Definierar applikationens navigerbara sektioner och deras språknycklar.</p>
 */
public enum NavigationItem {
    HOME(           "nav.home"),
    CUSTOMERS(      "nav.customers"),
    VEHICLES(       "nav.vehicles"),
    BOOKINGS(       "nav.bookings"),
    SERVICE_ITEMS(  "nav.serviceItems"),
    MECHANICS(      "nav.mechanics"),
    WORK_ORDERS(    "nav.workOrders"),
    INVOICES(       "nav.invoices"),
    PAYMENTS(       "nav.payments");

    private final String key;

    NavigationItem(String key){
        this.key = key;
    }

    public String getKey(){
        return key;
    }
}
