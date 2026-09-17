package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * <b>ProcessPaymentDialog</b>
 * <p>Ansvar: Dialog för att registrera en betalning.</p>
 */
public class ProcessPaymentDialog extends Dialog<ProcessPaymentDialog.Result> {

    private final Invoice invoice;

    public ProcessPaymentDialog(Invoice invoice) {
        this.invoice = invoice;

        this.setTitle("New Payment");
        this.setHeaderText("Register Payment");

        this.getDialogPane().getStylesheets().add(
                this.getClass().getResource("/com/wac/autocore/view/style.css").toExternalForm()
        );

        ComboBox<String> paymentTypeSelection = new ComboBox<>();

        paymentTypeSelection.getItems().addAll("CARD", "SWISH", "CASH");
        paymentTypeSelection.setPromptText("Payment type");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("Invoice #" + invoice.getId()),
                new Label("Total: " + invoice.getTotalAmount() + " SEK"),
                new Label("Payment type"), paymentTypeSelection
        );

        this.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        this.getDialogPane().lookupButton(saveButtonType).setDisable(true);

        paymentTypeSelection.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        this.getDialogPane().lookupButton(saveButtonType).setDisable(newValue == null)
        );

        this.setResultConverter(buttonType -> {

            if (buttonType != saveButtonType)
                return null;

            return new Result(this.invoice.getId(), paymentTypeSelection.getValue());
        });
    }

    public static class Result {

        private final int invoiceId;
        private final String paymentType;

        public Result(int invoiceId, String paymentType) {
            this.invoiceId = invoiceId;
            this.paymentType = paymentType;
        }

        public int getInvoiceId() {
            return invoiceId;
        }

        public String getPaymentType() {
            return paymentType;
        }
    }
}