package com.wac.autocore.view.dialog;

import com.wac.autocore.model.*;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.component.MultiSelectListView;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EditWorkOrderDialog extends Dialog<EditWorkOrderDialog.Result> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final WorkOrder workOrder;
    private final ServiceSelectorBox serviceSelectorBox;

    private final ComboBox<Mechanic> mechanicComboBox = new ComboBox<>();
    private final DatePicker plannedDatePicker = new DatePicker();
    private final TextArea customerInstructionsArea = new TextArea();
    private final TextArea commentsArea = new TextArea();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public EditWorkOrderDialog(WorkOrder workOrder, WorkOrderService workOrderService) {
        this.workOrder = workOrder;

        serviceSelectorBox = new ServiceSelectorBox(
                workOrderService.findAllServiceItems()
        );

        serviceSelectorBox.selectServiceIds(
                workOrder.getItems().stream()
                        .map(WorkOrderItem::getServiceItemId)
                        .collect(Collectors.toList())
        );

        setTitle(lang.get("workOrder.editTitle"));
        setHeaderText(lang.get("workOrder.editHeader"));

        DialogUtil.applyTheme(this);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        setupFields(workOrderService);
        buildContent();

        Button btnSave = (Button) getDialogPane().lookupButton(saveButtonType);
        btnSave.disableProperty().bind(serviceSelectorBox.emptyProperty());

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            return new Result(
                    serviceSelectorBox.getSelectedServiceIds(),
                    mechanicComboBox.getValue(),
                    plannedDatePicker.getValue(),
                    customerInstructionsArea.getText(),
                    commentsArea.getText()
            );
        });

    }

    private void setupFields(WorkOrderService workOrderService) {
        mechanicComboBox.getItems().setAll(workOrderService.findAllMechanics());
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
                    .filter(m -> m.getId() == workOrder.getMechanicId())
                    .findFirst()
                    .ifPresent(mechanicComboBox::setValue);
        }

        plannedDatePicker.setValue(workOrder.getPlannedDate());
        plannedDatePicker.setMaxWidth(Double.MAX_VALUE);

        customerInstructionsArea.setText(workOrder.getCustomerInstructions());
        customerInstructionsArea.setPromptText(lang.get("workOrder.customerInstructionsPrompt"));
        customerInstructionsArea.setWrapText(true);
        customerInstructionsArea.setPrefRowCount(3);

        commentsArea.setText(workOrder.getComments());
        commentsArea.setPromptText(lang.get("workOrder.commentsPrompt"));
        commentsArea.setWrapText(true);
        commentsArea.setPrefRowCount(3);
    }

    private void buildContent() {
        GridPane details = new GridPane();
        details.setHgap(12);
        details.setVgap(8);

        details.add(new Label(lang.get("table.mechanic")), 0, 0);
        details.add(mechanicComboBox, 1, 0);
        details.add(new Label(lang.get("workOrder.plannedDate")), 0, 1);
        details.add(plannedDatePicker, 1, 1);

        VBox content = new VBox(12,
                details,
                new Label(lang.get("workOrder.customerInstructions")),
                customerInstructionsArea,
                new Label(lang.get("workOrder.comments")),
                commentsArea,
                new Separator(),
                new Label(lang.get("table.serviceItems")),
                serviceSelectorBox
        );

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

        public Result(List<Integer> serviceItemIds, Mechanic mechanic,
                      LocalDate plannedDate,
                      String customerInstructions, String comments) {
            this.serviceItemIds = serviceItemIds;
            this.mechanic = mechanic;
            this.plannedDate = plannedDate;
            this.customerInstructions = customerInstructions;
            this.comments = comments;
        }

        public List<Integer> getServiceItemIds() { return serviceItemIds; }
        public Mechanic getMechanic() { return mechanic; }
        public LocalDate getPlannedDate() { return plannedDate; }
        public String getCustomerInstructions() { return customerInstructions; }
        public String getComments() { return comments; }
    }

}
