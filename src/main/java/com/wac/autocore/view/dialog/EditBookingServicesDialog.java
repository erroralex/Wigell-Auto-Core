package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.component.ServiceSelectorBox;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    private final ServiceSelectorBox serviceSelector;

    private static final LanguageManager lang =  LanguageManager.getInstance();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public EditBookingServicesDialog(Booking booking, BookingService bookingService) {
        this.booking = booking;
        this.bookingService = bookingService;

        // Initiera serviceSelector med alla tillgängliga tjänster
        serviceSelector = new ServiceSelectorBox(bookingService.listAllServiceItems());

        serviceSelector.selectServiceIds(
                booking.getItems().stream()
                        .map(item -> item.getServiceItemId())
                        .collect(Collectors.toList())
        );

        setTitle(lang.get("booking.editTitle"));
        setHeaderText(lang.get("booking.editHeader"));

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        buildInfoLabel();

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

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                infoLabel,
                new Separator(),
                new Label(lang.get("table.serviceItems")), serviceSelector
        );
        getDialogPane().setContent(content);
    }

    private void handleInput() {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(serviceSelector.emptyProperty());

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return new Result(serviceSelector.getSelectedServiceIds());
            }
            return null;
        });
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