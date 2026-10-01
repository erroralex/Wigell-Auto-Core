package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * <b>CreateBookingDialog</b>
 * <p>Ansvar: Dialog för att skapa en bokning.</p>
 */
public class CreateBookingDialog extends Dialog<CreateBookingDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final BookingService bookingService;

    private final ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<LocalTime> startTimeComboBox = new ComboBox<>();
    private final TextField descriptionTextField = new TextField();
    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final VBox serviceItemsBox = new VBox(8);
    private final Label errorLabel = new Label();
    private final Label estimatedTimeLabel = new Label("Upskattad Tid: ");
    private final Label basePriceLabel = new Label("Pris: ");
    private final List<Booking> bookingList;

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public CreateBookingDialog(BookingService bookingService) {
        this.bookingService = bookingService;

        this.bookingList = bookingService.listAll();

        errorLabel.getStyleClass().add("text-error");

        setTitle(lang.get("booking.new"));
        setHeaderText(lang.get("booking.create"));

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        vehicleComboBox.getItems().addAll(bookingService.listAllVehicles());
        mechanicComboBox.getItems().addAll(bookingService.listAllMechanics());
        populateStartTimes();
        populateServiceItems();

        setContent();
    }

    private void populateStartTimes() {
        LocalTime time = LocalTime.of(8, 0);
        LocalTime closing = LocalTime.of(17, 0);

        while (!time.isAfter(closing)) {
            startTimeComboBox.getItems().add(time);
            time = time.plusMinutes(30);
        }
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
            checkBox.selectedProperty().addListener((obs, was, is) -> updateTotals());
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

        // TODO använd lang
        if (selectedItems.isEmpty()) {
            estimatedTimeLabel.setText("Uppskattad Tid: ");
            basePriceLabel.setText("Pris: ");
            return;
        }

        int totalMinutes = selectedItems.stream()
                .mapToInt(ServiceItem::getEstimatedMinutes)
                .sum();

        estimatedTimeLabel.setText("Uppskattad Tid: " +  lang.get("format.minutes", totalMinutes)); // TODO använd lang

        double totalBasePrice = selectedItems.stream().mapToDouble(ServiceItem::getPrice).sum();
        basePriceLabel.setText("Pris: " +  totalBasePrice + " kr"); // TODO använd lang
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                errorLabel,
                new Label(lang.get("table.vehicle")), vehicleComboBox,
                new Label(lang.get("table.date")), datePicker,
                new Label(lang.get("table.startTime")), startTimeComboBox,
                new Label(lang.get("table.desc")), descriptionTextField,
                new Label(lang.get("table.mechanic")), mechanicComboBox,
                new Label(lang.get("table.serviceItem")), serviceItemsBox,
                estimatedTimeLabel,
                basePriceLabel
        );
        getDialogPane().setContent(content);

        handleInput();
    }

    private void handleInput() {

        setConverters();
        inputEventListeners();

        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {

            if (descriptionTextField.getText().isEmpty()
                    && vehicleComboBox.getValue() == null
                    && datePicker.getValue() == null
                    && startTimeComboBox.getValue() == null
                    && mechanicComboBox.getValue() == null
                    && getSelectedItems().isEmpty()) {
                errorLabel.setText(lang.get("error.fields"));
                vehicleComboBox.getStyleClass().add("input-error");
                datePicker.getStyleClass().add("input-error");
                startTimeComboBox.getStyleClass().add("input-error");
                descriptionTextField.getStyleClass().add("input-error");
                mechanicComboBox.getStyleClass().add("input-error");
                serviceItemsBox.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (vehicleComboBox.getValue() == null) {
                errorLabel.setText(lang.get("error.vehicleSelect"));
                vehicleComboBox.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (datePicker.getValue() == null) {
                errorLabel.setText(lang.get("error.dateSelect"));
                datePicker.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (startTimeComboBox.getValue() == null) {
                errorLabel.setText(lang.get("error.startTimeSelect"));
                startTimeComboBox.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (descriptionTextField.getText().isEmpty()) {
                errorLabel.setText(lang.get("error.desc"));
                descriptionTextField.getStyleClass().add("input-error");
                event.consume();
                return;
            }

            if (mechanicComboBox.getValue() == null) {
                errorLabel.setText(lang.get("error.mechanicSelect"));
                event.consume();
                return;
            }

            if (getSelectedItems().isEmpty()) {
                errorLabel.setText(lang.get("error.serviceSelect"));
                event.consume();
                return;
            }

            LocalDate date = datePicker.getValue();
            int vehicleId = vehicleComboBox.getValue().getId();

            if (bookingService.isVehicleBooked(vehicleId, date)) {
                AlertHelper.showError(
                        lang.get("error.booking"),
                        lang.get("error.vehicleBooked",
                                vehicleComboBox.getValue().getRegistrationNumber())
                );
                event.consume();
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {

                LocalTime startTime = startTimeComboBox.getValue();

                List<Integer> serviceItemIds = getSelectedItems().stream()
                        .map(ServiceItem::getId)
                        .collect(Collectors.toList());

                return new Result(
                        vehicleComboBox.getValue().getId(),
                        mechanicComboBox.getValue().getId(),
                        datePicker.getValue(),
                        startTime,
                        descriptionTextField.getText(),
                        serviceItemIds
                );
            }
            return null;
        });
    }

    private void inputEventListeners() {
        vehicleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                vehicleComboBox.getStyleClass().removeAll("input-error");
            }
        });

        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                datePicker.getStyleClass().removeAll("input-error");
            }
        });

        startTimeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                startTimeComboBox.getStyleClass().removeAll("input-error");
            }
        });

        descriptionTextField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                descriptionTextField.getStyleClass().removeAll("input-error");
            }
        });

        mechanicComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                mechanicComboBox.getStyleClass().removeAll("input-error");
            }
        });
    }

    private void setConverters() {

        vehicleComboBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                return v == null ? "" : v.getRegistrationNumber() + " - " + v.getBrand() + " " + v.getModel();
            }
            @Override
            public Vehicle fromString(String s) { return null; }
        });

        mechanicComboBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                return m == null ? "" : m.getName();
            }
            @Override
            public Mechanic fromString(String s) { return null; }
        });
    }

    public static class Result {
        private final int vehicleId;
        private final LocalDate date;
        private final String description;
        private final int mechanicId;
        private final LocalTime startTime;
        private final List<Integer> serviceItemIds;

        public Result(int vehicleId, int mechanicId, LocalDate date, LocalTime startTime, String description, List<Integer> serviceItemIds) {

            this.vehicleId = vehicleId;
            this.mechanicId = mechanicId;
            this.date = date;
            this.startTime = startTime;
            this.description = description;
            this.serviceItemIds = serviceItemIds;
        }

        public int getVehicleId() {
            return vehicleId;
        }

        public int getMechanicId() {
            return mechanicId;
        }

        public LocalDate getDate() {
            return date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public String getDescription() {
            return description;
        }

        public List<Integer> getServiceItemIds() {
            return serviceItemIds;
        }

    }
}