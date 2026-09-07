package com.wac.autocore.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * <b>SideNavigation</b>
 * <p>Ansvar: Bygger sidomenyns navigeringsknappar, markerar aktivt val och byter vy i {@link MainLayout} vid klick.</p>
 */
public class SideNavigation extends VBox {

    private final MainLayout mainLayout;
    private Button activeButton;

    public SideNavigation(MainLayout mainLayout) {
        this.mainLayout = mainLayout;

        this.getStyleClass().add("side_navigation");
        this.setSpacing(8);
        this.setPadding(new Insets(20));

        // Bygg en navigeringsknapp för varje sektion definierad i NavigationItem,
        // så nya sektioner läggs till bara genom att utöka enumen
        for (NavigationItem item : NavigationItem.values()) {
            Button btn = createNavButton(item);
            this.getChildren().add(btn);
        }

        // Visa första sektionen som vald vid uppstart
        if (!this.getChildren().isEmpty()) {
            setActiveButton((Button) this.getChildren().get(0));
        }
    }

    private Button createNavButton(NavigationItem item) {
        Button btn = new Button(item.getLabel());
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        // Byt innehåll i MainLayout och markera knappen som aktiv vid klick
        btn.setOnAction(e -> {
            mainLayout.setContent(resolveView(item));
            setActiveButton(btn);
        });
        return btn;
    }

    // Skapar en ny instans av rätt vy för varje NavigationItem.
    private Node resolveView(NavigationItem item) {
        switch (item) {
            case CUSTOMERS:     return new CustomerView();
            case VEHICLES:      return new VehicleView();
            case BOOKINGS:      return new BookingView();
            case SERVICE_ITEMS: return new ServiceItemView();
            case MECHANICS:     return new MechanicView();
            case WORK_ORDERS:   return new WorkOrderView();
            case INVOICES:      return new InvoiceView();
            case PAYMENTS:      return new PaymentView();
            default:            throw new IllegalStateException("Okänt NavigationItem: " + item);
        }
    }

    // Uppdaterar CSS-klass för att visuellt markera vilken sektion som är vald
    private void setActiveButton(Button newActiveButton) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        newActiveButton.getStyleClass().add("nav-button-active");
        activeButton = newActiveButton;
    }
}
