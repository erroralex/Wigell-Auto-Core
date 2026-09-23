package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Mechanic;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;



/**
 * <b>MechanicView</b>
 * <p>Ansvar: Visar och hanterar mekaniker i användargränssnittet.</p>
 */
public class MechanicView extends VBox {

    private final ObservableList<Mechanic> mechanicObservableList;
    private final VBox contentColumn = new VBox(20);
    private final VBox tableContainer = new VBox();
    private MechanicBookingsView bookingsView;

    public MechanicView() {
        this.mechanicObservableList = FXCollections.observableArrayList(Database.getMechanics());
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.TOP_LEFT);

        show();
    }

    private void show() {
        renderTitle();
        renderContentColumn();
    }

    private void renderTitle() {
        Label title = new Label("Mechanics");
        getChildren().add(title);
        title.getStyleClass().add("text-title");
    }

    private void renderContentColumn() {
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        VBox.setVgrow(contentColumn, Priority.ALWAYS);

        contentColumn.getChildren().add(tableContainer);
        getChildren().add(contentColumn);

        renderTable();
    }

    private void renderTable() {
        TableView<Mechanic> mechanicTableView = new TableView<>();

        mechanicTableView.setEditable(false);
        mechanicTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(mechanicTableView, Priority.ALWAYS);

        TableColumn<Mechanic, String> nameColumn = new TableColumn<>("Name");
        TableColumn<Mechanic, String> phoneColumn = new TableColumn<>("Phone Num");
        TableColumn<Mechanic, String> specColumn = new TableColumn<>("Specialization");
        TableColumn<Mechanic, String> availabilityColumn = new TableColumn<>("Available");
        TableColumn<Mechanic, Number> idColumn = new TableColumn<>("ID");

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

        idColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getId()));

        mechanicTableView.getColumns().add(nameColumn);
        mechanicTableView.getColumns().add(phoneColumn);
        mechanicTableView.getColumns().add(specColumn);
        mechanicTableView.getColumns().add(availabilityColumn);
        mechanicTableView.getColumns().add(idColumn);

        mechanicTableView.setItems(mechanicObservableList);

        mechanicTableView.setRowFactory(tv -> {
            TableRow<Mechanic> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showBookingsFor(row.getItem());
                }
            });
            return row;
        });

        tableContainer.getChildren().add(mechanicTableView);
    }

    private void showBookingsFor(Mechanic mechanic) {
        if (bookingsView == null) {
            bookingsView = new MechanicBookingsView(mechanic, () -> {
                contentColumn.getChildren().remove(bookingsView);
                bookingsView = null;
            });
           /* bookingsView.setPrefWidth(280);
            bookingsView.setMinWidth(200);*/
            HBox.setHgrow(bookingsView, Priority.NEVER);
            contentColumn.getChildren().add(bookingsView);
        } else {
            bookingsView.setMechanic(mechanic);
        }
    }
}
