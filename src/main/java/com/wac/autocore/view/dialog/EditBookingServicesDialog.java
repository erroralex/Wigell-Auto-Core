package com.wac.autocore.view.dialog;


import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.component.ServiceSelectorBox;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;


import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>EditBookingServicesDialog</b>
 * <p>Ansvar: Dialog för att redigera tjänster på en befintlig bokning.</p>
 */
public class EditBookingServicesDialog extends Dialog<EditBookingServicesDialog.Result> {

    private final Label errorLabel = new Label();
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

        errorLabel.getStyleClass().add("text-error");

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
                errorLabel,
                infoLabel,
                new Separator(),
                new Label(lang.get("table.serviceItems")), serviceSelector
        );
        getDialogPane().setContent(content);
    }

    private void handleInput() {
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            errorLabel.setText("");
            serviceSelector.setError(false);

            try {
                bookingService.update(
                        booking.getId(),
                        booking.getVehicleId(),
                        booking.getMechanicId(),
                        booking.getDate(),
                        booking.getStartTime(),
                        booking.getDescription(),
                        serviceSelector.getSelectedServiceIds()
                );
            } catch(RuntimeException e) {
                ErrorReport report = ErrorHandlerChain.createDefault().resolve(e);
                errorLabel.setText(lang.get(
                        report.getMessageKey(),
                        report.getMessageArgs()
                ));

                if ("error.serviceSelect".equals(report.getMessageKey())
                        || "error.serviceNotFound".equals(report.getMessageKey())
                        || "error.serviceItem.invalidDuration".equals(report.getMessageKey())
                ) {
                    serviceSelector.setError(true);
                }
                event.consume();
            }
        });

        setResultConverter(buttonType ->
                buttonType == saveButtonType ? new Result() : null);
    }

    public static class Result {
        private Result() {

        }
    }
}