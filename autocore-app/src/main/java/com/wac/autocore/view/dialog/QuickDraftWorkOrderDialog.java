package com.wac.autocore.view.dialog;

import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.component.CustomerVehiclePicker;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.Collections;

/**
 * <b>QuickDraftWorkOrderDialog</b>
 * <p>Responsibility: Quickly creating a drop-in draft with customer, vehicle and
 * problem description. The dialog saves by itself and returns the saved order.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class QuickDraftWorkOrderDialog extends FormDialog<DropInWorkOrder> {

    private final WorkOrderService workOrderService;
    private final CustomerVehiclePicker picker;
    private final StringBinding hint;

    private final Label problemLabel = new Label(lang.get("workOrder.problemDescription"));
    private final TextArea problemDescription = new TextArea();
    private final Label hintLabel = new Label();

    private DropInWorkOrder created;

    public QuickDraftWorkOrderDialog(WorkOrderService workOrderService, CustomerService customerService, VehicleService vehicleService) {
        this.workOrderService = workOrderService;
        this.picker = new CustomerVehiclePicker(customerService, vehicleService);

        problemDescription.setPromptText(lang.get("workOrder.problemDescription.prompt"));
        problemDescription.setWrapText(true);
        problemDescription.setPrefRowCount(3);

        hint = Bindings.createStringBinding(() -> {
                    String key = picker.missingSelectionKeyBinding().get();
                    return key == null ? null : lang.get(key);
                },
                picker.missingSelectionKeyBinding()
        );
        hintLabel.textProperty().bind(hint);
        hintLabel.visibleProperty().bind(hint.isNotNull());
        hintLabel.managedProperty().bind(hintLabel.visibleProperty());

        addEventHandler(DialogEvent.DIALOG_SHOWN, event -> picker.focusCustomer());

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "workOrder.quickDraft.title";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("workOrder.quickDraft.header");
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return hint.isNotNull();
    }

    @Override
    protected Node buildContent() {
        VBox content = new VBox();
        content.getChildren().addAll(picker, problemLabel, problemDescription, hintLabel);
        return content;
    }

    @Override
    protected void submit() {
        String description = problemDescription.getText();
        created = workOrderService.createDropIn(
                picker.getSelectedVehicle().getId(),
                description == null ? null : description.trim(),
                Collections.emptyList()
        );
    }

    @Override
    protected DropInWorkOrder buildResult() {
        return created;
    }
}
