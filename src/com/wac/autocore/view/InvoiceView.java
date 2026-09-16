package com.wac.autocore.view;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Observable;

/**
 * <b>InvoiceView</b>
 * <p>Ansvar: Visar och hanterar fakturor i användargränssnittet.</p>
 */
public class InvoiceView extends VBox {

    GarageSystem garageSystem = new GarageSystem();

    ObservableList<Invoice> invoiceMasterData = FXCollections.observableArrayList();

    TableView<Invoice> invoiceTable = new TableView<>();

    public InvoiceView() {

        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(invoiceTable, Priority.ALWAYS);

        Label title = new Label("Invoices");
        title.getStyleClass().add("text-title");

        this.getChildren().addAll(title, invoiceTable);
    }

}
