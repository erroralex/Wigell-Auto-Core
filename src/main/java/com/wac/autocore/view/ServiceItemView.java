package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.service.LanguageManager;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;


/**
 * <b>ServiceItemView</b>
 * <p>Ansvar: Visar och hanterar servicepunkter i användargränssnittet.</p>
 */
public class ServiceItemView extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final GarageSystem garageSystem;

    private final ObservableList<ServiceItem> serviceItemList;

    public ServiceItemView(GarageSystem garageSystem) {
        this.garageSystem = garageSystem;
        this.serviceItemList = FXCollections.observableArrayList(Database.getServiceItems());
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);

        show();
    }

    private void show() {
        renderTitle();
        renderTable();
    }

    private void renderTitle() {
        Label title = new Label(lang.get("serviceItem.title"));
        title.setId("h1");
        getChildren().add(title);
        title.getStyleClass().add("text-title");
    }

    private void renderTable() {
        TableView<ServiceItem> itemTableView = new TableView<>();

        itemTableView.setEditable(false);
        itemTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemTableView.setPlaceholder(new Label(lang.get("table.empty")));

        VBox.setVgrow(itemTableView, Priority.ALWAYS);

        TableColumn<ServiceItem, String> nameColumn = new TableColumn<>(lang.get("table.name"));
        TableColumn<ServiceItem, String> descColumn = new TableColumn<>(lang.get("table.desc"));
        TableColumn<ServiceItem, Double> priceColumn = new TableColumn<>(lang.get("table.price"));
        TableColumn<ServiceItem, Integer> durationColumn = new TableColumn<>(lang.get("table.estimatedDuration"));

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName())
        );

        descColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getDescription()));

        setPriceColumn(priceColumn);

        durationColumn.setCellValueFactory(cellData->
                new SimpleObjectProperty<>(cellData.getValue().getEstimatedMinutes()));

        itemTableView.getColumns().add(nameColumn);
        itemTableView.getColumns().add(descColumn);
        itemTableView.getColumns().add(priceColumn);
        itemTableView.getColumns().add(durationColumn);

        itemTableView.setItems(serviceItemList);

        getChildren().add(itemTableView);
    }

    private void setPriceColumn(TableColumn<ServiceItem, Double> priceColumn) {
        priceColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getPrice()));

        // Följer valt språk: 1 097,50 på svenska, 1,097.50 på engelska
        NumberFormat formatter = NumberFormat.getNumberInstance(lang.getLocale());
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        priceColumn.setCellFactory(column -> new TableCell<ServiceItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);

                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(formatter.format(price));
                }
            }
        });
    }
}
