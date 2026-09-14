package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;


/**
 * <b>ServiceItemView</b>
 * <p>Ansvar: Visar och hanterar servicepunkter i användargränssnittet.</p>
 */
public class ServiceItemView extends VBox {

    private final ObservableList<ServiceItem> serviceItemList;

    public ServiceItemView() {
        this.serviceItemList = FXCollections.observableArrayList(Database.getServiceItems());
        this.getStyleClass().add("service-item-view");

        show();
    }

    private void show() {
        renderTable();
    }

    private void renderTable() {
        TableView<ServiceItem> itemTableView = new TableView<>();

        itemTableView.setEditable(false);

        TableColumn<ServiceItem, String> nameColumn = new TableColumn<>("Namn");
        TableColumn<ServiceItem, String> descColumn = new TableColumn<>("Beskrivning");
        TableColumn<ServiceItem, Double> priceColumn = new TableColumn<>("Pris");
        TableColumn<ServiceItem, Integer> durationColumn = new TableColumn<>("");

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName())
        );

        descColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDescription()));

        priceColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getPrice()));

        durationColumn.setCellValueFactory(cellData->
                new SimpleObjectProperty<>(cellData.getValue().getEstimatedMinutes()));

        itemTableView.getColumns().add(nameColumn);
        itemTableView.getColumns().add(descColumn);
        itemTableView.getColumns().add(priceColumn);
        itemTableView.getColumns().add(durationColumn);

        itemTableView.setItems(serviceItemList);

        getChildren().add(itemTableView);
    }




}
