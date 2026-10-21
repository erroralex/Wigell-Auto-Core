package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>EditBookingServicesDialog</b>
 * <p>Ansvar: Dialog för att redigera tjänster på en befintlig bokning.</p>
 */
public class EditBookingServicesDialog extends Dialog<EditBookingServicesDialog.Result> {

    private final Booking booking;

    private final Label infoLabel = new Label();
    private final ServiceSelectorBox serviceSelector;

    private static final LanguageManager lang =  LanguageManager.getInstance();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public EditBookingServicesDialog(Booking booking, List<ServiceItem> serviceItems, Vehicle vehicle, Mechanic mechanic) {
        this.booking = booking;

        // Initiera serviceSelector med alla tillgängliga tjänster
        serviceSelector = new ServiceSelectorBox(serviceItems);

        serviceSelector.selectServiceIds(
                booking.getItems().stream()
                        .map(item -> item.getServiceItemId())
                        .collect(Collectors.toList())
        );

        setTitle(lang.get("booking.editTitle"));
        setHeaderText(lang.get("booking.editHeader"));

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        buildInfoLabel(vehicle, mechanic);

        setContent();
        handleInput();
    }

    private void buildInfoLabel(Vehicle vehicle, Mechanic mechanic) {

        String regNumber = vehicle != null ? vehicle.getRegistrationNumber() : "?";
        String mechanicName = booking.getMechanicId() == null ? "-"
                : mechanic != null ? mechanic.getName() : "?";

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