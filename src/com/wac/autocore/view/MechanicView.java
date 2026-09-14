package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Mechanic;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private final ObservableList<Mechanic> mechanicObservableList;

    public MechanicView() {
        this.mechanicObservableList = FXCollections.observableArrayList(Database.getMechanics());
        this.getStyleClass().add("mechanic-view");

        show();
    }

    private void show() {
        renderMechanics();
    }

    private void renderMechanics() {
        TableView<Mechanic> mechanicTableView = new TableView<>();

        mechanicTableView.setEditable(false);
        mechanicTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(mechanicTableView, Priority.ALWAYS);

        TableColumn<Mechanic, String> nameColumn = new TableColumn<>("Name");
        TableColumn<Mechanic, String> phoneColumn = new TableColumn<>("Phone Num");
        TableColumn<Mechanic, String> specColumn = new TableColumn<>("Specialization");
        TableColumn<Mechanic, String> availabilityColumn = new TableColumn<>("Available");

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
               return new SimpleStringProperty("Available");
           }
           return new SimpleStringProperty("Unavailable");
        });

        mechanicTableView.getColumns().add(nameColumn);
        mechanicTableView.getColumns().add(phoneColumn);
        mechanicTableView.getColumns().add(specColumn);
        mechanicTableView.getColumns().add(availabilityColumn);

        mechanicTableView.setItems(mechanicObservableList);

        getChildren().add(mechanicTableView);
    }
}
