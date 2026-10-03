package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

        // Binder titeln och headern till språknycklar med faktura-id
        titleProperty().bind(lang.bind("invoice.detailsTitle", invoice.getId()));
        headerTextProperty().bind(lang.bind("invoice.detailsHeader"));

        DialogUtil.applyTheme(this);

        // Lägger till en stäng-knapp som stänger dialogen med språknyckel
        ButtonType closeButtonType = new ButtonType(
                lang.get("btn.close"), ButtonBar.ButtonData.CANCEL_CLOSE
        );
        getDialogPane().getButtonTypes().add(closeButtonType);

        Button closeButton = (Button) getDialogPane().lookupButton(closeButtonType);
        closeButton.textProperty().bind(lang.bind("btn.close"));

        setContent();
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        // Skapar label för fakturadatum och work order-id med språkstöd
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
        amountCol.textProperty().bind(lang.bind("invoice.originalPrice"));
        amountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getAmount()).asObject()
        );

        TableColumn<InvoiceLine, Double> discountCol = new TableColumn<>();
        discountCol.textProperty().bind(lang.bind("table.discount"));
        discountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getDiscount()).asObject()
        );

        TableColumn<InvoiceLine, Double> totalCol = new TableColumn<>();
        totalCol.textProperty().bind(lang.bind("invoice.netPrice"));
        totalCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getTotal()).asObject()
        );

        // Konfigurera kolumnerna som visar pengar
        configureMoneyColumn(amountCol);
        configureMoneyColumn(discountCol);
        configureMoneyColumn(totalCol);

        table.getColumns().addAll(serviceCol, amountCol, discountCol, totalCol);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        // Sätter en placeholder med språkstöd när tabellen är tom
        Label placeholder = new Label();
        placeholder.textProperty().bind(lang.bind("table.empty"));
        table.setPlaceholder(placeholder);
        table.setItems(FXCollections.observableArrayList(invoice.getLines()));

        return table;
    }

    // Kolumn som visar pengar, med högerjustering och formatering
    private void configureMoneyColumn(TableColumn<InvoiceLine, Double> column) {
        column.setCellFactory(col -> new TableCell<InvoiceLine, Double>() {
            {
                setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);

                textProperty().unbind();
                setGraphic(null);

                if (empty || amount == null) {
                    setText(null);
                } else {
                    textProperty().bind(lang.bind("format.price", amount));
                }
            }
        });
    }
    // Skapar en label som visar en summering (t.ex. totalbelopp) med språkstöd
    private Label createSummaryLabel(String key, double amount) {
        Label label = new Label();
        label.textProperty().bind(Bindings.createStringBinding(
                () -> lang.get(key, lang.get("format.price", amount)),
                lang.localeProperty()
        ));
        return label;
    }
}