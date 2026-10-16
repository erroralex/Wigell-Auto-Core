package com.wac.autocore.view.dialog;

import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
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

    private final Booking previous;

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

        this.previous = previous;

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
            resetFieldErrorState();
            Vehicle vehicle = vehicleComboBox.getValue();
            Mechanic mechanic = mechanicComboBox.getValue();
            LocalDate date = datePicker.getValue();
            LocalTime startTime = startTimeComboBox.getValue();
            String description = descriptionTextField.getText();
            List<Integer> serviceIds = serviceSelector.getSelectedServiceIds();

            try {
                if (previous == null) {
                    bookingService.create(
                            vehicle == null ? null : vehicle.getId(),
                            mechanic == null ? null : mechanic.getId(),
                            date, startTime, description, serviceIds
                    );
                } else {
                    bookingService.createFromPrevious(
                            previous.getId(), date, startTime,
                            mechanic == null ? null : mechanic.getId(),
                            vehicle == null ? null : vehicle.getId(),
                            description, serviceIds);
                }
            } catch (RuntimeException e) {
                ErrorReport report = ErrorHandlerChain.createDefault().resolve(e);
                errorLabel.setText(lang.get(report.getMessageKey(), report.getMessageArgs()));
                markFieldError(report.getMessageKey());
                event.consume();
            }
        });

        setResultConverter(buttonType ->
                buttonType == saveButtonType ? new Result() : null);

    }

    private void markFieldError(String key) {
        switch (key) {
            case "error.vehicleSelect":
            case "error.vehicleBooked":
            case "error.vehicleNotFound":
                vehicleComboBox.getStyleClass().add("input-error");
                break;
            case "error.dateSelect":
                datePicker.getStyleClass().add("input-error");
                break;
            case "error.startTimeSelect":
                startTimeComboBox.getStyleClass().add("input-error");
                break;
            case "error.desc":
                descriptionTextField.getStyleClass().add("input-error");
                break;
            case "error.mechanicSelect":
            case "error.mechanicNotFound":
            case "error.mechanicBusy":
                mechanicComboBox.getStyleClass().add("input-error");
                break;
            case "error.serviceSelect":
                serviceSelector.setError(true);
                break;
            default:
                break;
        }
    }

    private void resetFieldErrorState() {
        errorLabel.setText("");
        vehicleComboBox.getStyleClass().remove("input-error");
        datePicker.getStyleClass().remove("input-error");
        startTimeComboBox.getStyleClass().remove("input-error");
        descriptionTextField.getStyleClass().remove("input-error");
        mechanicComboBox.getStyleClass().remove("input-error");
        serviceSelector.setError(false);
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

    /// Acts as a success marker.
    /// Save handler calls the service first, if it fails, it consumes the event and the dialog stays open.
    public static class Result {

        private Result() {
        }
    }
}