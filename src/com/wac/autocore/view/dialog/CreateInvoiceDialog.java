package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * <b>CreateInvoiceDialog</b>
 * <p>Ansvar: Dialog för att skapa en faktura.</p>
 */
public class CreateInvoiceDialog extends Dialog<CreateInvoiceDialog.Result> {

    public CreateInvoiceDialog() {
        this.setTitle("New Invoice");
        this.setHeaderText("Create Invoice");

        this.getDialogPane().getStylesheets().add(
                this.getClass().getResource("/com/wac/autocore/view/style.css").toExternalForm()
        );

        ComboBox<WorkOrder> workOrderSelection = new ComboBox<>();
        TextField discountCodeInput = new TextField();

        for (WorkOrder workOrder : Database.getWorkOrders()) {

            if (!"COMPLETED".equals(workOrder.getStatus()))
                continue;

            boolean alreadyInvoiced = false;

            for (Invoice invoice : Database.getInvoices()) {

                if (invoice.getWorkOrderId() == workOrder.getId())
                    alreadyInvoiced = true;
            }

            if (!alreadyInvoiced)
                workOrderSelection.getItems().add(workOrder);
        }

        workOrderSelection.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder workOrder) {
                if (workOrder == null)
                    return "";

                return "#" + workOrder.getId();
            }

            @Override
            public WorkOrder fromString(String text) {
                return null;
            }
        });

        discountCodeInput.setPromptText("Discount code (optional)");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("Work order"), workOrderSelection,
                new Label("Discount code"), discountCodeInput
        );

        this.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        this.getDialogPane().lookupButton(saveButtonType).setDisable(true);

        workOrderSelection.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        this.getDialogPane().lookupButton(saveButtonType).setDisable(newValue == null)
        );

        this.setResultConverter(buttonType -> {

            if (buttonType != saveButtonType)
                return null;

            return new Result(
                    workOrderSelection.getValue().getId(),
                    discountCodeInput.getText()
            );
        });
    }

    public static class Result {

        private final int workOrderId;
        private final String discountCode;

        public Result(int workOrderId, String discountCode) {
            this.workOrderId = workOrderId;
            this.discountCode = discountCode;
        }

        public int getWorkOrderId() {
            return workOrderId;
        }

        public String getDiscountCode() {
            return discountCode;
        }
    }
}