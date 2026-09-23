package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.LanguageManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;



/**
 * <b>MechanicView</b>
 * <p>Ansvar: Visar och hanterar mekaniker i användargränssnittet.</p>
 */
public class MechanicView extends VBox {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final ObservableList<Mechanic> mechanicObservableList;

    public MechanicView() {
        this.mechanicObservableList = FXCollections.observableArrayList(Database.getMechanics());
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
        Label title = new Label(lang.get("mechanic.title"));
        getChildren().add(title);
        title.getStyleClass().add("text-title");
    }

    private void renderTable() {
        TableView<Mechanic> mechanicTableView = new TableView<>();

        mechanicTableView.setEditable(false);
        mechanicTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mechanicTableView.setPlaceholder(new Label(lang.get("table.empty")));

        VBox.setVgrow(mechanicTableView, Priority.ALWAYS);

        TableColumn<Mechanic, String> nameColumn = new TableColumn<>(lang.get("table.name"));
        TableColumn<Mechanic, String> phoneColumn = new TableColumn<>(lang.get("table.phone"));
        TableColumn<Mechanic, String> specColumn = new TableColumn<>(lang.get("table.specialization"));
        TableColumn<Mechanic, String> availabilityColumn = new TableColumn<>(lang.get("table.availability"));
        TableColumn<Mechanic, Number> idColumn = new TableColumn<>(lang.get("table.id"));

        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName())
        );

        phoneColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone())
        );

        specColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSpecialization())
        );

        availabilityColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isAvailable()) {
                return new SimpleStringProperty(lang.get("mechanic.available"));
            }
            return new SimpleStringProperty(lang.get("mechanic.unavailable"));
        });

        idColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getId()));

        mechanicTableView.getColumns().add(nameColumn);
        mechanicTableView.getColumns().add(phoneColumn);
        mechanicTableView.getColumns().add(specColumn);
        mechanicTableView.getColumns().add(availabilityColumn);
        mechanicTableView.getColumns().add(idColumn);

        mechanicTableView.setItems(mechanicObservableList);

        getChildren().add(mechanicTableView);
    }
}