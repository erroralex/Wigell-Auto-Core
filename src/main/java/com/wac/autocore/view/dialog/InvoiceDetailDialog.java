package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * <b>InvoiceDetailDialog</b>
 * <p>Ansvar: Visar detaljer för en faktura – info, rader och summa.</p>
 */
public class InvoiceDetailDialog extends Dialog<Void> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final Invoice invoice;

    public InvoiceDetailDialog(Invoice invoice) {
        this.invoice = invoice;

        setTitle("Invoice details: " + " #" + invoice.getId()); // TODO lang
        setHeaderText("invoice details");   // TODO lang

        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        setContent();
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label dateLabel = new Label(lang.get("table.invoiceDate") + ": " + invoice.getInvoiceDate());
        Label workOrderLabel = new Label(lang.get("table.workOrderId") + ": #" + invoice.getWorkOrderId());

        TableView<InvoiceLine> table = createTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Label amountLabel = new Label(lang.get("table.amount") + ": " + invoice.getAmount() + " kr");
        Label discountLabel = new Label(lang.get("table.discount") + ": " + invoice.getDiscount() + " kr");
        Label totalLabel = new Label(lang.get("table.total") + ": " + invoice.getTotalAmount() + " kr");

        totalLabel.getStyleClass().add("text-title");

        content.getChildren().addAll(
                dateLabel, workOrderLabel,
                new Separator(),
                table,
                new Separator(),
                amountLabel, discountLabel, totalLabel
        );

        getDialogPane().setContent(content);
    }

    // Skapar tabellen som visar fakturaraderna, ändrat så att den använder språknyckeln för kolumnnamnen
    private TableView<InvoiceLine> createTable() {
        TableView<InvoiceLine> table = new TableView<>();

        TableColumn<InvoiceLine, String> serviceCol = new TableColumn<>();
        serviceCol.textProperty().bind(lang.bind("table.serviceItem"));
        serviceCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getServiceItemName())
        );

        TableColumn<InvoiceLine, Double> amountCol = new TableColumn<>();
        amountCol.textProperty().bind(lang.bind("table.amount"));
        amountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getAmount()).asObject()
        );

        TableColumn<InvoiceLine, Double> discountCol = new TableColumn<>();
        discountCol.textProperty().bind(lang.bind("table.discount"));
        discountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getDiscount()).asObject()
        );

        TableColumn<InvoiceLine, Double> totalCol = new TableColumn<>();
        totalCol.textProperty().bind(lang.bind("table.total"));
        totalCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getTotal()).asObject()
        );

        table.getColumns().addAll(serviceCol, amountCol, discountCol, totalCol);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(lang.get("table.empty")));
        table.setItems(FXCollections.observableArrayList(invoice.getLines()));

        return table;
    }
}