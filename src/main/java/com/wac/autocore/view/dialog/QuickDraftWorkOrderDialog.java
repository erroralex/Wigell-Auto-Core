package com.wac.autocore.view.dialog;

import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.component.CustomerVehiclePicker;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Collections;

/**
 * <b>QuickDraftWorkOrderDialog</b>
 * <p>Ansvar: Att snabbt skapa ett drop-in-utkast med kund, fordon och
 * problembeskrivning. Dialogen sparar själv och returnerar den sparade ordern.</p>
 */
public class QuickDraftWorkOrderDialog extends Dialog<DropInWorkOrder> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final CustomerVehiclePicker picker;
    private final Label problemLabel = new Label(lang.get("workOrder.problemDescription"));
    private final TextArea problemDescription = new TextArea();
    private final Label hintLabel = new Label();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    private DropInWorkOrder created;

    public QuickDraftWorkOrderDialog(WorkOrderService workOrderService, CustomerService customerService, VehicleService vehicleService) {

        this.picker = new CustomerVehiclePicker(customerService, vehicleService);

        DialogUtil.applyTheme(this);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        setTitle(lang.get("workOrder.quickDraft.title"));
        setHeaderText(lang.get("workOrder.quickDraft.header"));

        problemDescription.setPromptText(lang.get("workOrder.problemDescription.prompt"));
        problemDescription.setWrapText(true);
        problemDescription.setPrefRowCount(3);

        StringBinding hint = Bindings.createStringBinding(() -> {
                    String key = picker.missingSelectionKeyBinding().get();
                    return key == null ? null : lang.get(key);
                },
                picker.missingSelectionKeyBinding()
        );
        hintLabel.textProperty().bind(hint);
        hintLabel.visibleProperty().bind(hint.isNotNull());
        hintLabel.managedProperty().bind(hintLabel.visibleProperty());

        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        Button cancelButton = (Button) getDialogPane().lookupButton(cancelButtonType);
        saveButton.disableProperty().bind(hint.isNotNull());
        saveButton.getStyleClass().addAll("btn", "btn-primary");
        cancelButton.getStyleClass().addAll("btn", "btn-secondary");

        // Dialogen sparar själv; vid fel visas orsaken och dialogen stannar öppen.
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String description = problemDescription.getText();
            try {
                created = workOrderService.createDropIn(
                        picker.getSelectedVehicle().getId(),
                        description == null ? null : description.trim(),
                        Collections.emptyList()
                );
            } catch (RuntimeException e) {
                ErrorFacade.handle(e);
                event.consume();
            }
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(picker, problemLabel, problemDescription, hintLabel);
        getDialogPane().setContent(content);

        // Returnerar den sparade ordern; Avbryt ger null.
        setResultConverter(buttonType -> buttonType == saveButtonType ? created : null);

        setOnShown(event -> picker.focusCustomer());
    }
}
