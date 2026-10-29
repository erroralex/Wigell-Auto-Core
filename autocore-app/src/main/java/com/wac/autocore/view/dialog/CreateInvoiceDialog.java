package com.wac.autocore.view.dialog;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.view.util.StringConverterUtil;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * <b>CreateInvoiceDialog</b>
 * <p>Responsibility: Dialog for creating an invoice.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class CreateInvoiceDialog extends FormDialog<CreateInvoiceDialog.Result> {

    private final ComboBox<WorkOrder> workOrderSelection = new ComboBox<>();
    private final TextField discountCodeInput = new TextField();

    public CreateInvoiceDialog(WorkOrderService workOrderService) {
        workOrderSelection.getItems().addAll(workOrderService.findCompletedNotInvoiced());

        workOrderSelection.setConverter(StringConverterUtil.display(workOrder -> "#" + workOrder.getId()));

        discountCodeInput.setPromptText(lang.get("invoice.discountPrompt"));

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "invoice.new";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("invoice.create");
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return workOrderSelection.valueProperty().isNull();
    }

    @Override
    protected Node buildContent() {
        VBox content = new VBox();
        content.getChildren().addAll(
                new Label(lang.get("table.workOrder")), workOrderSelection,
                new Label(lang.get("table.discountCode")), discountCodeInput
        );

        return content;
    }

    @Override
    protected void submit() {
    }

    @Override
    protected Result buildResult() {
        return new Result(workOrderSelection.getValue().getId(), discountCodeInput.getText());
    }

    public static class Result {

        private final Integer workOrderId;
        private final String discountCode;

        public Result(Integer workOrderId, String discountCode) {
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
