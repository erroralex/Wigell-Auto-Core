package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.view.util.DialogUtil;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/**
 * <b>InvoiceDetailDialog</b>
 * <p>Ansvar: Visar detaljer för en faktura.</p>
 */
public class InvoiceDetailDialog extends Dialog<Void> {

    private final Invoice invoice;

    public InvoiceDetailDialog(Invoice invoice) {
        this.invoice = invoice;

        setTitle("Faktura #" + invoice.getId());
        setHeaderText("Detaljer");

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}