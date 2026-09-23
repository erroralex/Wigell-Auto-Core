package com.wac.autocore.view;

import com.wac.autocore.service.LanguageManager;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * <b>MainLayout</b>
 * <p>Ansvar: Applikationens huvudskal. Håller sidonavigeringen, växlar vilken vy som visas i mitten och bygger om aktuell vy vid språkbyte.</p>
 */
public class MainLayout extends BorderPane {

    private final SideNavigation sideNavigation;
    private NavigationItem currentItem;

    public MainLayout() {
        this.getStyleClass().add("main-layout");

        // Bygger om aktuell vy när språket växlas, så vyer utan bindningar också byter språk
        LanguageManager.getInstance().localeProperty()
                .addListener((obs, oldLocale, newLocale) -> {
                    if (currentItem != null) {
                        setCenter(createView(currentItem));
                    }
                });

        // SideNavigation väljer första sektionen och anropar show() under uppbyggnaden
        this.sideNavigation = new SideNavigation(this);
        this.setLeft(sideNavigation);
    }

    /** Byter central sektion. Anropas av SideNavigation vid varje sektionsbyte. */
    public void show(NavigationItem item) {
        this.currentItem = item;
        setCenter(createView(item));
    }

    // Skapar en ny instans av rätt vy för varje NavigationItem.
    private Node createView(NavigationItem item) {
        switch (item) {
            case HOME:          return new HomeView();
            case CUSTOMERS:     return new CustomerView();
            case VEHICLES:      return new VehicleView();
            case BOOKINGS:      return new BookingView();
            case SERVICE_ITEMS: return new ServiceItemView();
            case MECHANICS:     return new MechanicView();
            case WORK_ORDERS:   return new WorkOrderView();
            case INVOICES:      return new InvoiceView();
            case PAYMENTS:      return new PaymentView();
            default:            throw new IllegalStateException("Unknown NavigationItem: " + item);
        }
    }
}