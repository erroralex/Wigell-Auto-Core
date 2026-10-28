package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.view.util.StringConverterUtil;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * <b>ProcessPaymentDialog</b>
 * <p>Responsibility: Dialog for registering a payment.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class ProcessPaymentDialog extends FormDialog<ProcessPaymentDialog.Result> {

    private final Invoice invoice;

    private final ComboBox<String> paymentTypeSelection = new ComboBox<>();

    public ProcessPaymentDialog(Invoice invoice) {
        this.invoice = invoice;

        // The values are stored as they are (CARD/SWISH/CASH), only the display is translated
        paymentTypeSelection.getItems().addAll("CARD", "SWISH", "CASH");
        paymentTypeSelection.setPromptText(lang.get("table.paymentType"));
        paymentTypeSelection.setConverter(StringConverterUtil.display(type -> lang.get("payment.type." + type)));

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "payment.new";
    }

    @Override
    protected String buildHeaderText() {
        return lang.get("payment.register");
    }

    @Override
    protected ObservableBooleanValue saveDisabled() {
        return paymentTypeSelection.valueProperty().isNull();
    }

    @Override
    protected Node buildContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label(lang.get("payment.invoice", invoice.getId())),
                new Label(lang.get("payment.total", invoice.getTotalAmount())),
                new Label(lang.get("table.paymentType")), paymentTypeSelection
        );
        return content;
    }

    @Override
    protected void submit() {
    }

    @Override
    protected Result buildResult() {
        return new Result(invoice.getId(), paymentTypeSelection.getValue());
    }

    public static class Result {

        private final Integer invoiceId;
        private final String paymentType;

        public Result(Integer invoiceId, String paymentType) {
            this.invoiceId = invoiceId;
            this.paymentType = paymentType;
        }

        public Integer getInvoiceId() {
            return invoiceId;
        }

        public String getPaymentType() {
            return paymentType;
        }
    }
}
