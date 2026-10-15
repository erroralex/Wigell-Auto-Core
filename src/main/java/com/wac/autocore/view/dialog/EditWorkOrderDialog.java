package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class EditWorkOrderDialog extends Dialog<EditWorkOrderDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final WorkOrder workOrder;
    private final ServiceSelectorBox serviceSelectorBox;

    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final DatePicker plannedDatePicker = new DatePicker();
    private final TextArea customerInstructionsArea = new TextArea();
    private final TextArea commentsArea = new TextArea();

    private final Map<Integer, CheckBox> chargeBoxes = new LinkedHashMap<>();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public EditWorkOrderDialog(WorkOrder workOrder, List<ServiceItem> serviceItems, List<Mechanic> mechanics) {
        this.workOrder = workOrder;

        serviceSelectorBox = new ServiceSelectorBox(serviceItems);

        serviceSelectorBox.selectServiceIds(
                workOrder.getItems().stream()
                        .map(WorkOrderItem::getServiceItemId)
                        .collect(Collectors.toList())
        );

        setTitle(lang.get("workOrder.edit.title"));
        setHeaderText(lang.get("workOrder.edit.header"));

        DialogUtil.applyTheme(this);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        setupFields(mechanics);

        if (workOrder.allowsNonChargeableItems()) {
            for (WorkOrderItem item : workOrder.getItems()) {
                CheckBox cb = new CheckBox(item.getServiceName());
                cb.setSelected(item.isChargeable());
                chargeBoxes.put(item.getServiceItemId(), cb);
            }
        }
        buildContent();

        Button btnSave = (Button) getDialogPane().lookupButton(saveButtonType);
        btnSave.disableProperty().bind(serviceSelectorBox.emptyProperty());

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            Map<Integer, Boolean> flags = new HashMap<>();
            chargeBoxes.forEach((id, cb) -> flags.put(id, cb.isSelected()));

            return new Result(
                    serviceSelectorBox.getSelectedServiceIds(),
                    mechanicComboBox.getValue(),
                    plannedDatePicker.getValue(),
                    customerInstructionsArea.getText(),
                    commentsArea.getText(),
                    flags
            );
        });

    }

    private void setupFields(List<Mechanic> mechanics) {
        mechanicComboBox.getItems().setAll(mechanics);
        mechanicComboBox.setMaxWidth(Double.MAX_VALUE);
        mechanicComboBox.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic mechanic) {
                return mechanic == null ? "" : mechanic.toString();
            }

            @Override
            public Mechanic fromString(String string) {
                return null;
            }
        });

        if (workOrder.getMechanicId() != null) {
            mechanicComboBox.getItems().stream()
                    .filter(m -> Objects.equals(m.getId(), workOrder.getMechanicId()))
                    .findFirst()
                    .ifPresent(mechanicComboBox::setValue);
        }

        plannedDatePicker.setValue(workOrder.getPlannedDate());
        plannedDatePicker.setMaxWidth(Double.MAX_VALUE);

        customerInstructionsArea.setText(workOrder.getCustomerInstructions());
        customerInstructionsArea.setPromptText(lang.get("workOrder.edit.customerInstructionsPrompt"));
        customerInstructionsArea.setWrapText(true);
        customerInstructionsArea.setPrefRowCount(3);

        commentsArea.setText(workOrder.getComments());
        commentsArea.setPromptText(lang.get("workOrder.edit.commentsPrompt"));
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(3);
    }

    private void buildContent() {
        GridPane details = new GridPane();
        details.setHgap(12);
        details.setVgap(8);

        details.add(new Label(lang.get("table.mechanic")), 0, 0);
        details.add(mechanicComboBox, 1, 0);
        details.add(new Label(lang.get("workOrder.edit.plannedDate")), 0, 1);
        details.add(plannedDatePicker, 1, 1);

        VBox content = new VBox(12,
                details,
                new Label(lang.get("workOrder.edit.customerInstructions")),
                customerInstructionsArea,
                new Label(lang.get("workOrder.edit.comments")),
                commentsArea,
                new Separator(),
                new Label(lang.get("table.serviceItems")),
                serviceSelectorBox
        );

        if (!chargeBoxes.isEmpty()) {
            content.getChildren().add(new Label(lang.get("workOrder.edit.chargeCustomer")));
            content.getChildren().addAll(chargeBoxes.values());
        }

        content.setPadding(new Insets(12));
        content.setPrefWidth(560);

        getDialogPane().setContent(content);
    }


    public static class Result {

        private final List<Integer> serviceItemIds;
        private final Mechanic mechanic;
        private final LocalDate plannedDate;
        private final String customerInstructions;
        private final String comments;

        private final Map<Integer, Boolean> chargeFlags;

        public Result(List<Integer> serviceItemIds, Mechanic mechanic,
                      LocalDate plannedDate,
                      String customerInstructions, String comments,
                      Map<Integer, Boolean> chargeFlags) {
            this.serviceItemIds = serviceItemIds;
            this.mechanic = mechanic;
            this.plannedDate = plannedDate;
            this.customerInstructions = customerInstructions;
            this.comments = comments;
            this.chargeFlags = chargeFlags;
        }

        public List<Integer> getServiceItemIds() {
            return serviceItemIds;
        }

        public Mechanic getMechanic() {
            return mechanic;
        }

        public LocalDate getPlannedDate() {
            return plannedDate;
        }

        public String getCustomerInstructions() {
            return customerInstructions;
        }

        public String getComments() {
            return comments;
        }

        public Map<Integer, Boolean> getChargeFlags() {
            return chargeFlags;
        }
    }

}
