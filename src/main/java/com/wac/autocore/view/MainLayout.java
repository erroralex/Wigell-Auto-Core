package com.wac.autocore.view;

import com.wac.autocore.service.*;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class MainLayout extends BorderPane {

    private final GarageSystem garageSystem;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final WorkOrderService workOrderService;
    private final ServiceItemService serviceItemService;
    private final SideNavigation sideNavigation;
    private NavigationItem currentItem;

    public MainLayout(GarageSystem garageSystem,
                      InvoiceService invoiceService,
                      PaymentService paymentService,
                      BookingService bookingService,
                      WorkOrderService workOrderService,
                      ServiceItemService serviceItemService) {
        this.garageSystem = garageSystem;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.workOrderService = workOrderService;
        this.serviceItemService = serviceItemService;
        this.getStyleClass().add("main-layout");

        LanguageManager.getInstance().localeProperty()
                .addListener((obs, oldLocale, newLocale) -> {
                    if (currentItem != null) {
                        setCenter(createView(currentItem));
                    }
                });

        this.sideNavigation = new SideNavigation(this);
        this.setLeft(sideNavigation);
    }

    public void show(NavigationItem item) {
        this.currentItem = item;
        setCenter(createView(item));
    }

    private Node createView(NavigationItem item) {
        switch (item) {
            case HOME:          return new HomeView();
            case CUSTOMERS:     return new CustomerView(garageSystem);
            case VEHICLES:      return new VehicleView(garageSystem);
            case BOOKINGS:      return new BookingView(bookingService);
            case SERVICE_ITEMS: return new ServiceItemView(serviceItemService);
            case MECHANICS:     return new MechanicView(garageSystem);
            case WORK_ORDERS:   return new WorkOrderView(workOrderService);
            case INVOICES:      return new InvoiceView(invoiceService, workOrderService);
            case PAYMENTS:      return new PaymentView(invoiceService, paymentService);
            default:            throw new IllegalStateException("Unknown NavigationItem: " + item);
        }
    }
}