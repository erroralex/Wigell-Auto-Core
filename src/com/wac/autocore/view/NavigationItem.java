package com.wac.autocore.view;

/**
 * <b>NavigationItem</b>
 * <p>Ansvar: Definierar applikationens navigerbara sektioner och deras visningsetiketter.</p>
 */
public enum NavigationItem {
    CUSTOMERS(      "Kunder"),
    VEHICLES(       "Fordon"),
    BOOKINGS(       "Bokningar"),
    SERVICE_ITEMS(  "Servicepunkter"),
    MECHANICS(      "Mekaniker"),
    WORK_ORDERS(    "Arbetsordrar"),
    INVOICES(       "Fakturor"),
    PAYMENTS(       "Betalningar");

    private final String label;

    NavigationItem(String label){
        this.label = label;
    }

    public String getLabel(){
        return label;
    }
}
