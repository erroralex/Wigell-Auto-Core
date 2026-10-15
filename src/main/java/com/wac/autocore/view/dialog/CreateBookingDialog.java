package com.wac.autocore.view.dialog;

import com.wac.autocore.model.*;
import com.wac.autocore.service.BookingPriceChange;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.component.ServicePackageSelectorBox;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


/**
 * <b>CreateBookingDialog</b>
 * <p>Ansvar: Dialog för att skapa en bokning.</p>
 */
public class CreateBookingDialog extends Dialog<CreateBookingDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final BookingService bookingService;
    private final ServicePackageSelectorBox servicePackageSelector;

    private final ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<LocalTime> startTimeComboBox = new ComboBox<>();
    private final TextField descriptionTextField = new TextField();
    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final Label errorLabel = new Label();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
    private final ServiceSelectorBox serviceSelector;

    private VBox contentBox;

    public CreateBookingDialog(BookingService bookingService, ServicePackageService servicePackageService) {
        this(bookingService, servicePackageService, null);
    }

    public CreateBookingDialog(BookingService bookingService,
                               ServicePackageService servicePackageService,
                               Booking previous) {
        this.bookingService = bookingService;

        this.servicePackageSelector = new ServicePackageSelectorBox(
                servicePackageService.findAll());

        this.serviceSelector = new ServiceSelectorBox(bookingService.listAllServiceItems());

        errorLabel.getStyleClass().add("text-error");

        setTitle(lang.get("booking.new"));
        setHeaderText(lang.get("booking.create"));

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        vehicleComboBox.getItems().addAll(bookingService.listAllVehicles());
        mechanicComboBox.getItems().addAll(bookingService.listAllMechanics());
        populateStartTimes();

        setContent();

        if (previous != null) {
            prefillFrom(previous);
        }
    }

    private void populateStartTimes() {
        LocalTime time = LocalTime.of(8, 0);
        LocalTime closing = LocalTime.of(17, 0);

        while (!time.isAfter(closing)) {
            startTimeComboBox.getItems().add(time);
            time = time.plusMinutes(30);
        }
    }

    private void prefillFrom(Booking previous) {
        vehicleComboBox.getItems().stream()
                .filter(v -> v.getId() == previous.getVehicleId())
                .findFirst()
                .ifPresent(vehicleComboBox::setValue);

        mechanicComboBox.getItems().stream()
                .filter(m -> m.getId() == previous.getMechanicId())
                .findFirst()
                .ifPresent(mechanicComboBox::setValue);

        descriptionTextField.setText(previous.getDescription());

        serviceSelector.selectServiceIds(
                bookingService.listServiceItemIds(previous.getId()));

        showPriceChangeNotice(bookingService.findPriceChanges(previous.getId()));
    }

    private void showPriceChangeNotice(List<BookingPriceChange> changes) {
        if (changes.isEmpty()) return;

        StringBuilder sb = new StringBuilder(lang.get("booking.priceChangedNotice"));
        for (BookingPriceChange c : changes) {
            sb.append("\n• ").append(c.getServiceName())
                    .append(": ").append(DialogUtil.formatMoney(c.getPreviousPrice()))
                    .append(" → ").append(DialogUtil.formatMoney(c.getCurrentPrice()));
        }

        Label notice = new Label(sb.toString());
        notice.setWrapText(true);
        notice.getStyleClass().add("text-warning");

        contentBox.getChildren().add(1, notice);
    }

    private void setContent() {
        contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16));
        contentBox.getChildren().addAll(
                errorLabel,
                new Label(lang.get("table.vehicle")), vehicleComboBox,
                new Label(lang.get("table.date")), datePicker,
                new Label(lang.get("table.startTime")), startTimeComboBox,
                new Label(lang.get("table.desc")), descriptionTextField,
                new Label(lang.get("table.mechanic")), mechanicComboBox,
                new Label(lang.get("servicePackage.label")), servicePackageSelector,
                new Label(lang.get("table.serviceItem")), serviceSelector
        );
        getDialogPane().setContent(contentBox);

        handleInput();
    }

    private void handleInput() {

        setConverters();
        inputEventListeners();

        servicePackageSelector.selectedPackageProperty().addListener((observable, oldPackage, newPackage) -> {
            if (newPackage != null) {
                List<Integer> ids = new ArrayList<>();
                for (ServiceItem service : newPackage.getServices()) {
                    ids.add(service.getId());
                }
                serviceSelector.selectServiceIds(ids);
            }
        });

        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().addAll("btn", "btn-primary");

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {

            if (descriptionTextField.getText().isEmpty()
                    && vehicleComboBox.getValue() == null
                    && datePicker.getValue() == null
                    && startTimeComboBox.getValue() == null
                    && mechanicComboBox.getValue() == null
                    && serviceSelector.isEmpty()) {
                // Bytte ut tidigare checkbox-check mot ServiceSelectorBox: isEmpty() som hanterar valet av tjänster.
                errorLabel.setText(lang.get("error.fields"));
                vehicleComboBox.getStyleClass().add("input-error");
                datePicker.getStyleClass().add("input-error");
                startTimeComboBox.getStyleClass().add("input-error");
                descriptionTextField.getStyleClass().add("input-error");
                mechanicComboBox.getStyleClass().add("input-error");
                serviceSelector.setError(true);
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

            if (serviceSelector.isEmpty()) {
                errorLabel.setText(lang.get("error.serviceSelect"));
                serviceSelector.setError(true);
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

                List<Integer> serviceItemIds = serviceSelector.getSelectedServiceIds();

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