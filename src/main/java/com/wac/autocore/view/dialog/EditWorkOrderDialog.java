package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.component.MultiSelectListView;
import com.wac.autocore.view.component.ServiceSelectorBox;
import com.wac.autocore.view.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

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

    private final VBox warrantyOptions = new VBox(8);
    private final Label totalPriceLabel = new Label();
    private final Label totalDurationLabel = new Label();

    private final ButtonType saveButtonType =
            new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType =
            new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private final Map<Integer, CheckBox> chargeableCheckboxes = new HashMap<>();
    private final Map<Integer, WorkOrderItem> existingItems = new HashMap<>();

    public EditWorkOrderDialog(WorkOrder workOrder, WorkOrderService workOrderService) {
        this.workOrder = workOrder;

        serviceSelectorBox = new ServiceSelectorBox(workOrderService.findAllServiceItems());

        workOrder.getItems().forEach(item ->
                existingItems.put(item.getServiceItemId(), item));

        serviceSelectorBox.selectServiceIds(
                workOrder.getItems().stream()
                        .map(WorkOrderItem::getServiceItemId)
                        .collect(Collectors.toList())
        );

        setTitle(lang.get("workOrder.editTitle"));
        setHeaderText(lang.get("workOrder.editHeader"));

        DialogUtil.applyTheme(this);
        getDialogPane().getButtonTypes().addAll(
                saveButtonType, cancelButtonType
        );

        setupFields(workOrderService);
        buildContent();

        setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            return new Result();
        });
    }

    private void setupFields(WorkOrderService workOrderService) {
        mechanicComboBox.getItems().setAll(
                workOrderService.findAllMechanics()
        );

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
                    .filter(mechanic ->
                            mechanic.getId() == workOrder.getMechanicId())
                    .findFirst()
                    .ifPresent(mechanicComboBox::setValue);
        }

        plannedDatePicker.setValue(workOrder.getPlannedDate());
        plannedDatePicker.setMaxWidth(Double.MAX_VALUE);

        customerInstructionsArea.setText(
                workOrder.getCustomerInstructions()
        );
        customerInstructionsArea.setPromptText(
                lang.get("workOrder.customerInstructionsPrompt")
        );
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
                serviceSelectorBox,
                warrantyOptions,
                new Separator(),
                totalPriceLabel,
                totalDurationLabel);

        content.setPadding(new Insets(12));
        content.setPrefWidth(560);

        getDialogPane().setContent(content);
    }




    public static class Result {


    }

}
