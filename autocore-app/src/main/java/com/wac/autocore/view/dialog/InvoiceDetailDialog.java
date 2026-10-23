package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * <b>InvoiceDetailDialog</b>
 * <p>Responsibility: Shows details for an invoice – info, lines and total.</p>
 */
public class InvoiceDetailDialog extends Dialog<Void> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final Invoice invoice;

    public InvoiceDetailDialog(Invoice invoice) {
        this.invoice = invoice;

        // Binds the title and the header to language keys with the invoice id
        titleProperty().bind(lang.bind("invoice.detailsTitle", invoice.getId()));
        headerTextProperty().bind(lang.bind("invoice.detailsHeader"));

        DialogUtil.applyTheme(this);

        // Adds a close button that closes the dialog, with a language key
        ButtonType closeButtonType = new ButtonType(
                lang.get("btn.close"), ButtonBar.ButtonData.CANCEL_CLOSE
        );
        getDialogPane().getButtonTypes().add(closeButtonType);

        Button closeButton = (Button) getDialogPane().lookupButton(closeButtonType);
        closeButton.textProperty().bind(lang.bind("btn.close"));

        setContent();
    }

    private void setContent() {
        VBox content = new VBox();
        content.getStyleClass().add("invoice-detail-content");

        // Creates labels for the invoice date and work order id with language support
        Label dateLabel = new Label();
        dateLabel.textProperty().bind(
                lang.bind("table.invoiceDate")
                        .concat(": ")
                        .concat(invoice.getInvoiceDate().toString())
        );

        Label workOrderLabel = new Label();
        workOrderLabel.textProperty().bind(
                lang.bind("table.workOrderId")
                        .concat(": #")
                        .concat(Integer.toString(invoice.getWorkOrderId()))
        );

        TableView<InvoiceLine> table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Label amountLabel = createSummaryLabel(
                "invoice.subtotal", invoice.getAmount()
        );
        Label discountLabel = createSummaryLabel(
                "invoice.totalDiscount", invoice.getDiscount()
        );
        Label totalLabel = createSummaryLabel(
                "invoice.grandTotal", invoice.getTotalAmount()
        );

        // Adds CSS classes for the summary labels
        totalLabel.getStyleClass().add("invoice-grand-total");

        VBox summary = new VBox(amountLabel, discountLabel, totalLabel);
        summary.getStyleClass().add("invoice-summary");

        amountLabel.setWrapText(true);
        discountLabel.setWrapText(true);
        totalLabel.setWrapText(true);

        content.getChildren().addAll(
                dateLabel, workOrderLabel,
                new Separator(),
                table,
                new Separator(),
                summary
        );

        getDialogPane().setContent(content);
    }

    // Creates the table that shows the invoice lines, changed so it uses the language key for the column names
    private TableView<InvoiceLine> createTable() {
        TableView<InvoiceLine> table = new TableView<>();
        table.getStyleClass().add("invoice-lines-table"); // CSS class for the table

        TableColumn<InvoiceLine, String> serviceCol = new TableColumn<>();
        serviceCol.textProperty().bind(lang.bind("table.serviceItem"));
        serviceCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getServiceItemName())
        );

        TableColumn<InvoiceLine, Money> amountCol = new TableColumn<>();
        amountCol.textProperty().bind(lang.bind("invoice.originalPrice"));
        amountCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getAmount())
        );

        TableColumn<InvoiceLine, Money> discountCol = new TableColumn<>();
        discountCol.textProperty().bind(lang.bind("table.discount"));
        discountCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getDiscount())
        );

        TableColumn<InvoiceLine, Money> totalCol = new TableColumn<>();
        totalCol.textProperty().bind(lang.bind("invoice.netPrice"));
        totalCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getTotal())
        );

        // Configure the columns that show money, with currency
        DialogUtil.configureMoneyColumn(amountCol, true);
        DialogUtil.configureMoneyColumn(discountCol, true);
        DialogUtil.configureMoneyColumn(totalCol, true);

        table.getColumns().addAll(serviceCol, amountCol, discountCol, totalCol);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Sets a placeholder with language support when the table is empty
        Label placeholder = new Label();
        placeholder.textProperty().bind(lang.bind("table.empty"));
        table.setPlaceholder(placeholder);
        table.setItems(FXCollections.observableArrayList(invoice.getLines()));

        return table;
    }

    // Creates a label that shows a summary (e.g. total amount) with language support
    private Label createSummaryLabel(    String key, Money amount) {
        Label label = new Label();
        label.textProperty().bind(Bindings.createStringBinding(
                    () -> lang.get(key, lang.get("format.price", amount)),
                lang.localeProperty()
        ));
        return label;
    }
}