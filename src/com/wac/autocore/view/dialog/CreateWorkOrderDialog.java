package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
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

    private final ComboBox<Booking> bookingCombo = new ComboBox<>();
    private final ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
    private final ListView<ServiceItem> serviceItemList = new ListView<>();
    private final Label totalLabel = new Label("Sum: 0.00 kr");

    private final ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

    public CreateWorkOrderDialog() {
        setTitle("New Work Order");
        setHeaderText("Create Work Order");

        getDialogPane().getStylesheets().add(getClass().getResource("/com/wac/autocore/view/style.css").toExternalForm());

        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // -------------------------------------------------------------------------------------------------------------

        bookingCombo.getItems().addAll(Database.getBookings());
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
                        ? "" : item.getName() + " - " + String.format("%.2f kr", item.getPrice()));
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

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("Bokning"), bookingCombo,
                new Label("Mekaniker"), mechanicCombo,
                new Label("Servicepunkter"), serviceItemList,
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
        totalLabel.setText(String.format("Summa: %.2f kr", total));
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
