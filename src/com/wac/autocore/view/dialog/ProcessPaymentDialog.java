package com.wac.autocore.view.dialog;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * <b>ProcessPaymentDialog</b>
 * <p>Ansvar: Dialog för att registrera en betalning.</p>
 */
public class ProcessPaymentDialog extends Dialog<ProcessPaymentDialog.Result> {

    public ProcessPaymentDialog() {
        this.setTitle("New Payment");
        this.setHeaderText("Register Payment");

        this.getDialogPane().getStylesheets().add(
                this.getClass().getResource("/com/wac/autocore/view/style.css").toExternalForm()
        );

        ComboBox<Invoice> invoiceSelection = new ComboBox<>();
        ComboBox<String> paymentTypeSelection = new ComboBox<>();

        for (Invoice invoice : Database.getInvoices()) {

            if (!invoice.isPaid())
                invoiceSelection.getItems().add(invoice);
        }

        invoiceSelection.setConverter(new StringConverter<Invoice>() {
            @Override
            public String toString(Invoice invoice) {
                if (invoice == null)
                    return "";

                return "#" + invoice.getId() + " - " + invoice.getTotalAmount() + " SEK";
            }

            @Override
            public Invoice fromString(String text) {
                return null;
            }
        });

        paymentTypeSelection.getItems().addAll("CARD", "SWISH", "CASH");
        paymentTypeSelection.setPromptText("Payment type");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label("Invoice"), invoiceSelection,
                new Label("Payment type"), paymentTypeSelection
        );

        this.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        this.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        this.getDialogPane().lookupButton(saveButtonType).setDisable(true);

        invoiceSelection.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        this.updateSaveButtonState(saveButtonType, invoiceSelection, paymentTypeSelection)
        );

        paymentTypeSelection.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        this.updateSaveButtonState(saveButtonType, invoiceSelection, paymentTypeSelection)
        );

        this.setResultConverter(buttonType -> {

            if (buttonType != saveButtonType)
                return null;

            return new Result(
                    invoiceSelection.getValue().getId(),
                    paymentTypeSelection.getValue()
            );
        });
    }

    private void updateSaveButtonState(ButtonType saveButtonType,
                                       ComboBox<Invoice> invoiceSelection,
                                       ComboBox<String> paymentTypeSelection) {

        boolean hasInvoice = invoiceSelection.getValue() != null;
        boolean hasType = paymentTypeSelection.getValue() != null;

        this.getDialogPane().lookupButton(saveButtonType).setDisable(!hasInvoice || !hasType);
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