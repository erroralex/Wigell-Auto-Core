package com.wac.autocore.view;

/**
 * <b>NavigationItem</b>
 * <p>Ansvar: Definierar applikationens navigerbara sektioner och deras visningsetiketter.</p>
 */
public enum NavigationItem {
    CUSTOMERS(      "Customers"),
    VEHICLES(       "Vehicles"),
    BOOKINGS(       "Bookings"),
    SERVICE_ITEMS(  "Service Items"),
    MECHANICS(      "Mechanics"),
    WORK_ORDERS(    "Work Orders"),
    INVOICES(       "Invoices"),
    PAYMENTS(       "Payments"),;

    private final String label;

    NavigationItem(String label){
        this.label = label;
    }

    public String getLabel(){
        return label;
    }
}
