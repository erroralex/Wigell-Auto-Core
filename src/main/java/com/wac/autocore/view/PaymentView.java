package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.dialog.ProcessPaymentDialog;
import com.wac.autocore.view.util.AlertHelper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * <b>PaymentView</b>
 * <p>Ansvar: Visar och hanterar betalningar i användargränssnittet.</p>
 */
public class PaymentView extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final GarageSystem garageSystem = new GarageSystem();

    private final ObservableList<Invoice> invoiceMasterData = FXCollections.observableArrayList();
    private final SortedList<Invoice> sortedData = new SortedList<>(invoiceMasterData);

    private final TableView<Invoice> invoiceTable = new TableView<>();

    private final Button btnPay = new Button(lang.get("btn.pay"));

    public PaymentView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(invoiceTable, Priority.ALWAYS);

        Label title = new Label(lang.get("payment.title"));
        title.getStyleClass().add("text-title");

        this.loadMasterData();
        this.initializeTable();

        HBox buttonBar = this.createButtonBar();

        this.invoiceTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) ->
                        this.updateButtonState(newValue)
        );

        this.updateButtonState(null);

        this.getChildren().addAll(title, buttonBar, invoiceTable);
    }

    private void loadMasterData() {
        this.invoiceMasterData.setAll(Database.getInvoices());
    }

    private void refreshData() {
        this.invoiceMasterData.setAll(Database.getInvoices());
    }

    private void initializeTable() {

        TableColumn<Invoice, Integer> idCol = new TableColumn<>(lang.get("table.invoiceId"));
        idCol.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getId()).asObject()
        );

        TableColumn<Invoice, Double> amountCol = new TableColumn<>(lang.get("table.amount"));
        amountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getAmount()).asObject()
        );

        TableColumn<Invoice, Double> discountCol = new TableColumn<>(lang.get("table.discount"));
        discountCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getDiscount()).asObject()
        );

        TableColumn<Invoice, Double> totalCol = new TableColumn<>(lang.get("table.total"));
        totalCol.setCellValueFactory(c ->
                new SimpleDoubleProperty(c.getValue().getTotalAmount()).asObject()
        );

        TableColumn<Invoice, String> typeCol = new TableColumn<>(lang.get("table.paymentType"));
        typeCol.setCellValueFactory(c -> {
            Payment payment = this.findPayment(c.getValue());

            if (payment == null)
                return new SimpleStringProperty("–");

            // Samma nycklar som i ProcessPaymentDialog: CARD/SWISH/CASH översätts vid visning
            return new SimpleStringProperty(lang.get("payment.type." + payment.getPaymentType()));
        });

        TableColumn<Invoice, String> successfulCol = new TableColumn<>(lang.get("table.successful"));
        successfulCol.setCellValueFactory(c -> {
            Payment payment = this.findPayment(c.getValue());

            if (payment == null)
                return new SimpleStringProperty("–");

            return new SimpleStringProperty(payment.isSuccessful() ? lang.get("common.yes") : lang.get("common.no"));
        });

        TableColumn<Invoice, String> dateCol = new TableColumn<>(lang.get("table.date"));
        dateCol.setCellValueFactory(c -> {
            Payment payment = this.findPayment(c.getValue());

            if (payment == null || payment.getPaymentDate() == null)
                return new SimpleStringProperty("–");

            return new SimpleStringProperty(
                    payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            );
        });

        this.invoiceTable.getColumns().addAll(
                idCol, amountCol, discountCol, totalCol, typeCol, successfulCol, dateCol
        );

        this.invoiceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.invoiceTable.setPlaceholder(new Label(lang.get("table.empty")));

        this.sortedData.comparatorProperty().bind(this.invoiceTable.comparatorProperty());
        this.invoiceTable.setItems(this.sortedData);
    }

    private Payment findPayment(Invoice invoice) {

        for (Payment payment : Database.getPayments()) {

            if (payment.getInvoiceId() == invoice.getId())
                return payment;
        }

        return null;
    }

    private HBox createButtonBar() {
        btnPay.getStyleClass().addAll("btn", "btn-primary");
        btnPay.setOnAction(event -> this.openProcessPaymentDialog());

        HBox box = new HBox(15, btnPay);
        box.setPadding(new Insets(15, 0, 0, 0));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void updateButtonState(Invoice selected) {

        if (selected == null) {
            btnPay.setDisable(true);
            return;
        }

        btnPay.setDisable(selected.isPaid());
    }

    private void openProcessPaymentDialog() {

        Invoice selected = this.invoiceTable.getSelectionModel().getSelectedItem();

        if (selected == null)
            return;

        ProcessPaymentDialog dialog = new ProcessPaymentDialog(selected);

        dialog.showAndWait().ifPresent(result -> {

            Payment payment = garageSystem.processPayment(
                    result.getInvoiceId(),
                    result.getPaymentType()
            );

            if (payment != null && payment.isSuccessful()) {
                refreshData();
                AlertHelper.showInfo(lang.get("payment.registered"), lang.get("payment.registeredMsg"));
            }

            else {
                AlertHelper.showError(lang.get("error.payment"), lang.get("error.paymentProcess"));
            }
        });
    }
}