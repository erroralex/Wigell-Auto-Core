package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.view.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>EditBookingServicesDialog</b>
 * <p>Ansvar: Dialog för att redigera tjänster på en befintlig bokning.</p>
 */
public class EditBookingServicesDialog extends Dialog<EditBookingServicesDialog.Result> {

    private final BookingService bookingService;
    private final Booking booking;

    private final Label infoLabel = new Label();
    private final VBox serviceItemsBox = new VBox(8);
    private final Label estimatedTimeLabel = new Label("Uppskattad Tid: ");
    private final Label basePriceLabel = new Label("Pris: ");

    private final ButtonType saveButtonType = new ButtonType("Spara", ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType("Avbryt", ButtonBar.ButtonData.CANCEL_CLOSE);

    public EditBookingServicesDialog(Booking booking, BookingService bookingService) {
        this.booking = booking;
        this.bookingService = bookingService;

        setTitle("Redigera bokning");
        setHeaderText("Redigera tjänster");

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        buildInfoLabel();
        populateServiceItems();
        updateTotals();

        setContent();
        handleInput();
    }

    private void buildInfoLabel() {
        Map<Integer, Vehicle> vehicleMap = bookingService.listAllVehicles().stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v));

        Map<Integer, Mechanic> mechanicMap = bookingService.listAllMechanics().stream()
                .collect(Collectors.toMap(Mechanic::getId, m -> m));

        Vehicle vehicle = vehicleMap.get(booking.getVehicleId());
        Mechanic mechanic = mechanicMap.get(booking.getMechanicId());

        String regNumber = vehicle != null ? vehicle.getRegistrationNumber() : "?";
        String mechanicName = mechanic != null ? mechanic.getName() : "?";

        String info = regNumber + " | " + mechanicName + " | " +
                booking.getDate() + " " +
                booking.getStartTime();

        infoLabel.setText(info);
        infoLabel.getStyleClass().add("text-secondary");
    }

    private void populateServiceItems() {
        serviceItemsBox.setPadding(new Insets(8));
        serviceItemsBox.getStyleClass().add("service-items-box");

        for (ServiceItem serviceItem : bookingService.listAllServiceItems()) {
            CheckBox checkBox = new CheckBox(
                    serviceItem.getName() + " – " + serviceItem.getPrice() + " kr (" +
                            serviceItem.getEstimatedMinutes() + " min)"
            );
            checkBox.setUserData(serviceItem);

            boolean alreadySelected = booking.getItems().stream()
                    .anyMatch(item -> item.getServiceItemId() == serviceItem.getId());
            checkBox.setSelected(alreadySelected);

            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                updateTotals();
                updateSaveButton();
            });

            serviceItemsBox.getChildren().add(checkBox);
        }
    }

    private List<ServiceItem> getSelectedItems() {
        List<ServiceItem> selectedItems = new ArrayList<>();

        for (Node node : serviceItemsBox.getChildren()) {
            if (node instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) node;
                if (checkBox.isSelected()) {
                    selectedItems.add((ServiceItem) checkBox.getUserData());
                }
            }
        }

        return selectedItems;
    }

    private void updateTotals() {
        List<ServiceItem> selectedItems = getSelectedItems();

        if (selectedItems.isEmpty()) {
            estimatedTimeLabel.setText("Uppskattad Tid: ");
            basePriceLabel.setText("Pris: ");
            return;
        }

        int totalMinutes = selectedItems.stream()
                .mapToInt(ServiceItem::getEstimatedMinutes)
                .sum();

        estimatedTimeLabel.setText("Uppskattad Tid: " + totalMinutes + " min");

        double totalBasePrice = selectedItems.stream()
                .mapToDouble(ServiceItem::getPrice)
                .sum();

        basePriceLabel.setText("Pris: " + totalBasePrice + " kr");
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                infoLabel,
                new Separator(),
                new Label("Tjänster:"), serviceItemsBox,
                estimatedTimeLabel,
                basePriceLabel
        );
        getDialogPane().setContent(content);
    }

    private void handleInput() {
        updateSaveButton();

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                List<Integer> serviceItemIds = getSelectedItems().stream()
                        .map(ServiceItem::getId)
                        .collect(Collectors.toList());

                return new Result(serviceItemIds);
            }
            return null;
        });
    }

    private void updateSaveButton() {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(getSelectedItems().isEmpty());
    }

    public static class Result {
        private final List<Integer> serviceItemIds;

        public Result(List<Integer> serviceItemIds) {
            this.serviceItemIds = serviceItemIds;
        }

        public List<Integer> getServiceItemIds() {
            return serviceItemIds;
        }
    }
}