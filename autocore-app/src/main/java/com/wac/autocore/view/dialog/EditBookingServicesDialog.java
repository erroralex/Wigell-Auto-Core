package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.view.component.ServiceSelectorBox;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>EditBookingServicesDialog</b>
 * <p>Responsibility: Dialog for editing the services on an existing booking.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class EditBookingServicesDialog extends FormDialog<EditBookingServicesDialog.Result> {

    private final Booking booking;

    private final Label infoLabel = new Label();
    private final ServiceSelectorBox serviceSelector;

    public EditBookingServicesDialog(Booking booking, List<ServiceItem> serviceItems, Vehicle vehicle, Mechanic mechanic) {
        this.booking = booking;

        serviceSelector = new ServiceSelectorBox(serviceItems);

        serviceSelector.selectServiceIds(
                booking.getItems().stream()
                        .map(item -> item.getServiceItemId())
                        .collect(Collectors.toList())
        );

        buildInfoLabel(vehicle, mechanic);

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "booking.editTitle";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("booking.editHeader");
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return serviceSelector.emptyProperty();
    }

    @Override
    protected Node buildContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                infoLabel,
                new Separator(),
                new Label(lang.get("table.serviceItems")), serviceSelector
        );
        return content;
    }

    @Override
    protected void submit() {
    }

    @Override
    protected Result buildResult() {
        return new Result(serviceSelector.getSelectedServiceIds());
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
