package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
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

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final Invoice invoice;

    public ProcessPaymentDialog(Invoice invoice) {
        this.invoice = invoice;

        this.setTitle(lang.get("payment.new"));
        this.setHeaderText(lang.get("payment.register"));

        DialogUtil.applyTheme(this);

        ComboBox<String> paymentTypeSelection = new ComboBox<>();

        // Värdena sparas som de är (CARD/SWISH/CASH), bara visningen översätts
        paymentTypeSelection.getItems().addAll("CARD", "SWISH", "CASH");
        paymentTypeSelection.setPromptText(lang.get("table.paymentType"));
        paymentTypeSelection.setConverter(new StringConverter<String>() {

            @Override
            public String toString(String type) {
                return type == null ? "" : lang.get("payment.type." + type);
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label(lang.get("payment.invoice", invoice.getId())),
                new Label(lang.get("payment.total", invoice.getTotalAmount())),
                new Label(lang.get("table.paymentType")), paymentTypeSelection
        );

        this.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        this.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

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