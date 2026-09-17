package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.view.dialog.CreateInvoiceDialog;
import com.wac.autocore.view.util.AlertHelper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
 * <b>InvoiceView</b>
 * <p>Ansvar: Visar och hanterar fakturor i användargränssnittet.</p>
 */
public class InvoiceView extends VBox {

    private final GarageSystem garageSystem = new GarageSystem();

    private final ObservableList<Invoice> invoiceMasterData = FXCollections.observableArrayList();
    private final SortedList<Invoice> sortedData = new SortedList<>(invoiceMasterData);

    private final TableView<Invoice> invoiceTable = new TableView<>();

    private final Button btnCreateInvoice = new Button("Create");

    public InvoiceView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(invoiceTable, Priority.ALWAYS);

        Label title = new Label("Invoices");
        title.getStyleClass().add("text-title");

        this.loadMasterData();
        this.initializeTable();

        HBox buttonBar = this.createButtonBar();

        this.getChildren().addAll(title, buttonBar, invoiceTable);
    }

    private void loadMasterData() {
        this.invoiceMasterData.setAll(Database.getInvoices());
    }

    private void refreshData() {
        this.invoiceMasterData.setAll(Database.getInvoices());
    }

    private void initializeTable() {

        TableColumn<Invoice, Integer> idCol = new TableColumn<>("Invoice-ID");
        idCol.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getId()).asObject()
        );

        TableColumn<Invoice, Integer> workOrderIdCol = new TableColumn<>("Order-ID");
        workOrderIdCol.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getWorkOrderId()).asObject()
        );

        TableColumn<Invoice, LocalDate> dateCol = new TableColumn<>("Invoice Date");
        dateCol.setCellValueFactory(c ->
                new SimpleObjectProperty<>(c.getValue().getInvoiceDate())
        );

        TableColumn<Invoice, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getAmount()).asObject()
        );

        TableColumn<Invoice, Double> discountCol = new TableColumn<>("Discount");
        discountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getDiscount()).asObject()
        );

        TableColumn<Invoice, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject()
        );

        TableColumn<Invoice, Boolean> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(c ->
                new SimpleBooleanProperty(c.getValue().isPaid()).asObject()
        );

        paidCol.setCellFactory(col -> new TableCell<Invoice, Boolean>() {
            @Override
            protected void updateItem(Boolean paid, boolean empty) {
                super.updateItem(paid, empty);
                if (empty || paid == null) {
                    setText(null);
                } else {
                    setText(paid ? "Yes" : "No");
                }
            }
        });

        this.invoiceTable.getColumns().addAll(
                idCol, workOrderIdCol, dateCol, amountCol, discountCol, totalCol, paidCol
        );

        this.invoiceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        this.sortedData.comparatorProperty().bind(this.invoiceTable.comparatorProperty());
        this.invoiceTable.setItems(this.sortedData);
    }

    private HBox createButtonBar() {
        String btnPrimary = "btn-primary";

        btnCreateInvoice.getStyleClass().addAll("btn", btnPrimary);
        btnCreateInvoice.setOnAction(event -> this.openCreateInvoiceDialog());

        HBox box = new HBox(15, btnCreateInvoice);
        box.setPadding(new Insets(15, 0, 0, 0));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void openCreateInvoiceDialog() {
        CreateInvoiceDialog dialog = new CreateInvoiceDialog();

        dialog.showAndWait().ifPresent(result -> {

            Invoice invoice = garageSystem.createInvoice(
                    result.getWorkOrderId(),
                    result.getDiscountCode()
            );

            if (invoice != null) {
                refreshData();
                AlertHelper.showInfo("Invoice created", "A new invoice has been created");
            }

            else {
                AlertHelper.showError("Could not create", "Check that work order is COMPLETED");
            }
        });
    }
}