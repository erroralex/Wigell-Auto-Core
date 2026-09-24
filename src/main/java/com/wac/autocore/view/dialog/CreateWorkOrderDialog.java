package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * <b>CreateWorkOrderDialog</b>
 * <p>Ansvar: Dialog för att skapa en arbetsorder.</p>
 */
public class CreateWorkOrderDialog extends Dialog<CreateWorkOrderDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final ComboBox<Booking> bookingCombo = new ComboBox<>();
    private final ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
    private final ListView<ServiceItem> serviceItemList = new ListView<>();
    private final Label totalLabel = new Label();

    private static final String BOOKING_STATUS_BOOKED = "BOOKED";

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public CreateWorkOrderDialog() {
        setTitle(lang.get("workOrder.new"));
        setHeaderText(lang.get("workOrder.create"));

        // Hämta css-styling och applicera på nya dialog:
        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        // -------------------------------------------------------------------------------------------------------------

        for (Booking booking : Database.getBookings()) {
            if (BOOKING_STATUS_BOOKED.equals(booking.getStatus())) {
                bookingCombo.getItems().add(booking);
            }
        }

        bookingCombo.setConverter(new StringConverter<Booking>() {

            @Override
            public String toString(Booking booking) {
                return booking == null ? "" : "#" + booking.getId() + " - " + booking.getDescription();
            }

            @Override
            public Booking fromString(String string) {
                return null;
            }
        });

        // -------------------------------------------------------------------------------------------------------------

        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.isAvailable()) {
                mechanicCombo.getItems().add(mechanic);
            }
        }
        mechanicCombo.setConverter(new StringConverter<Mechanic>() {

            @Override
            public String toString(Mechanic mechanic) {
                return mechanic == null ? "" : mechanic.getName();
            }

            @Override
            public Mechanic fromString(String string) {
                return null;
            }
        });

        // -------------------------------------------------------------------------------------------------------------

        serviceItemList.getItems().addAll(Database.getServiceItems());
        serviceItemList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serviceItemList.setPrefHeight(160);
        serviceItemList.setCellFactory(list -> new ListCell<ServiceItem>() {

            @Override
            protected void updateItem(ServiceItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? "" : item.getName() + " - " + lang.get("format.price", item.getPrice()));
            }
        });

        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Summa och validering uppdateras löpande vid varje val
        serviceItemList.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<ServiceItem>) change -> {
                    updateTotal();
                    validate(saveButton);
                });
        bookingCombo.valueProperty().addListener((obs, oldV, newV) -> validate(saveButton));
        mechanicCombo.valueProperty().addListener((obs, oldV, newV) -> validate(saveButton));

        // Sätter startvärdet (0) via samma nyckel som vid uppdatering
        updateTotal();

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label(lang.get("table.booking")), bookingCombo,
                new Label(lang.get("table.mechanic")), mechanicCombo,
                new Label(lang.get("table.serviceItems")), serviceItemList,
                totalLabel
        );
        getDialogPane().setContent(content);

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                int bookingId = bookingCombo.getValue().getId();
                int mechanicId = mechanicCombo.getValue().getId();
                int[] serviceItemIds = serviceItemList.getSelectionModel()
                        .getSelectedItems()
                        .stream()
                        .mapToInt(ServiceItem::getId)
                        .toArray();
                return new Result(bookingId, mechanicId, serviceItemIds);
            }
            return null;
        });
    }

    private void updateTotal() {
        double total = serviceItemList.getSelectionModel()
                .getSelectedItems()
                .stream()
                .mapToDouble(ServiceItem::getPrice)
                .sum();
        totalLabel.setText(lang.get("workOrder.total", total));
    }

    private void validate(Node saveButton) {
        boolean valid = bookingCombo.getValue() != null
                && mechanicCombo.getValue() != null
                && !serviceItemList.getSelectionModel().getSelectedItems().isEmpty();
        saveButton.setDisable(!valid);
    }

// -----------------------------------------------------------------------------------------------------------------

    public static class Result {
        private final int bookingId;
        private final int mechanicId;
        private final int[] serviceItemIds;

        public Result(int bookingId, int mechanicId, int[] serviceItemIds) {
            this.bookingId = bookingId;
            this.mechanicId = mechanicId;
            this.serviceItemIds = serviceItemIds;
        }

        public int getBookingId() {
            return bookingId;
        }

        public int getMechanicId() {
            return mechanicId;
        }

        public int[] getServiceItemIds() {
            return serviceItemIds;
        }
    }
}