package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
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

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final InvoiceService invoiceService;

    public CreateInvoiceDialog(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
        this.setTitle(lang.get("invoice.new"));
        this.setHeaderText(lang.get("invoice.create"));

        DialogUtil.applyTheme(this);

        ComboBox<WorkOrder> workOrderSelection = new ComboBox<>();
        TextField discountCodeInput = new TextField();

        for (WorkOrder workOrder : Database.getWorkOrders()) {

            if (!"COMPLETED".equals(workOrder.getStatus()))
                continue;

            boolean alreadyInvoiced = false;

            for (Invoice invoice : invoiceService.findAll()) {

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

        discountCodeInput.setPromptText(lang.get("invoice.discountPrompt"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label(lang.get("table.workOrder")), workOrderSelection,
                new Label(lang.get("table.discountCode")), discountCodeInput
        );

        this.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

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