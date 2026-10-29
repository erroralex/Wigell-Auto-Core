package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingPriceChange;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.component.ServicePackageSelectorBox;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>CreateBookingDialog</b>
 * <p>Responsibility: Dialog for creating a booking.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class CreateBookingDialog extends FormDialog<CreateBookingDialog.Result> {

    private final BookingService bookingService;
    private final ServicePackageSelectorBox servicePackageSelector;
    private final ServiceSelectorBox serviceSelector;

    private final ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<LocalTime> startTimeComboBox = new ComboBox<>();
    private final TextField descriptionTextField = new TextField();
    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final Label priceNoticeLabel = new Label();

    // previous may be null
    public CreateBookingDialog(
            BookingService bookingService,
            ServicePackageService servicePackageService,
            List<Vehicle> vehicles,
            List<Mechanic> mechanics,
            List<ServiceItem> serviceItems,
            Booking previous
    ) {
        this.bookingService = bookingService;
        this.servicePackageSelector = new ServicePackageSelectorBox(servicePackageService.findAll());
        this.serviceSelector = new ServiceSelectorBox(serviceItems);

        vehicleComboBox.getItems().addAll(vehicles);
        mechanicComboBox.getItems().addAll(mechanics);
        populateStartTimes();
        configureInputs();
        configurePriceNotice();

        if (previous != null) {
            prefillFrom(previous);
        }

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "booking.new";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("booking.create");
    }

    @Override
    protected Node buildContent() {
        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(16));
        contentBox.getChildren().addAll(
                priceNoticeLabel,
                new Label(lang.get("table.vehicle")), vehicleComboBox,
                new Label(lang.get("table.date")), datePicker,
                new Label(lang.get("table.startTime")), startTimeComboBox,
                new Label(lang.get("table.desc")), descriptionTextField,
                new Label(lang.get("table.suggestedMechanic")), suggestedMechanicBox(),
                new Label(lang.get("servicePackage.label")), servicePackageSelector,
                new Label(lang.get("table.serviceItem")), serviceSelector
        );
        return contentBox;
    }

    @Override
    protected void submit() {
    }

    @Override
    protected Result buildResult() {
        Vehicle vehicle = vehicleComboBox.getValue();
        Mechanic mechanic = mechanicComboBox.getValue();

        return new Result(
                vehicle == null ? null : vehicle.getId(),
                mechanic == null ? null : mechanic.getId(),
                datePicker.getValue(),
                startTimeComboBox.getValue(),
                descriptionTextField.getText(),
                serviceSelector.getSelectedServiceIds()
        );
    }

    private void configureInputs() {
        setConverters();

        servicePackageSelector.selectedPackageProperty()
                .addListener((
                        observable,
                        oldPackage,
                        newPackage) -> {
                    if (newPackage == null) {
                        return;
                    }

                    List<Integer> serviceIds = new ArrayList<>();
                    for (ServiceItem service : newPackage.getServices()) {
                        serviceIds.add(service.getId());
                    }
                    serviceSelector.selectServiceIds(serviceIds);
                });
    }

    // Hidden until a copied booking has price changes
    private void configurePriceNotice() {
        priceNoticeLabel.setWrapText(true);
        priceNoticeLabel.getStyleClass().add("text-warning");
        priceNoticeLabel.setVisible(false);
        priceNoticeLabel.managedProperty().bind(priceNoticeLabel.visibleProperty());
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
                .filter(m -> Objects.equals(m.getId(), previous.getMechanicId()))
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

        priceNoticeLabel.setText(sb.toString());
        priceNoticeLabel.setVisible(true);
    }

    // The suggestion is optional, so a selected suggestion can be cleared without cancelling the dialog.
    private HBox suggestedMechanicBox() {
        Button clearButton = new Button(lang.get("btn.remove"));
        clearButton.getStyleClass().addAll("btn", "btn-secondary");
        clearButton.disableProperty().bind(mechanicComboBox.valueProperty().isNull());
        clearButton.setOnAction(event -> mechanicComboBox.setValue(null));

        mechanicComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mechanicComboBox, Priority.ALWAYS);
        return new HBox(8, mechanicComboBox, clearButton);
    }

    private void setConverters() {
        vehicleComboBox.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle v) {
                return v == null ? "" : v.getRegistrationNumber() + " - " + v.getBrand() + " " + v.getModel();
            }

            @Override
            public Vehicle fromString(String s) {
                return null;
            }
        });

        mechanicComboBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic m) {
                return m == null ? "" : m.getName();
            }

            @Override
            public Mechanic fromString(String s) {
                return null;
            }
        });
    }

    public static class Result {
        private final Integer vehicleId;
        private final LocalDate date;
        private final String description;
        // Optional suggestion; null = no suggested mechanic.
        private final Integer mechanicId;
        private final LocalTime startTime;
        private final List<Integer> serviceItemIds;

        public Result(Integer vehicleId, Integer mechanicId, LocalDate date, LocalTime startTime, String description, List<Integer> serviceItemIds) {
            this.vehicleId = vehicleId;
            this.mechanicId = mechanicId;
            this.date = date;
            this.startTime = startTime;
            this.description = description;
            this.serviceItemIds = serviceItemIds;
        }

        public Integer getVehicleId() {
            return vehicleId;
        }

        public Integer getMechanicId() {
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
