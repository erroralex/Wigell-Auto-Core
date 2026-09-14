package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.view.dialog.CreateCustomerDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * <b>CustomerView</b>
 * <p>Ansvar: Visar och hanterar kundinformation i användargränssnittet.</p>
 */
public class CustomerView extends VBox {

    private final TableView<Customer> customerTable = new TableView<>();
    private final ObservableList<Customer> masterData = FXCollections.observableArrayList();
    private final FilteredList<Customer> filteredData = new FilteredList<>(masterData, customer -> true);
    private final TextField searchField = new TextField();

    public CustomerView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(customerTable, Priority.ALWAYS);

        Label title = new Label("Customers");
        title.getStyleClass().add("text-title");

        loadMasterData();
        initializeTable();
        configureSearch();

        this.getChildren().addAll(title, createToolbar(), customerTable);
    }

    private void loadMasterData() {
        masterData.setAll(Database.getCustomers());
    }

    private void refreshData() {
        masterData.setAll(Database.getCustomers());
        applyFilter(searchField.getText());
    }

    private void initializeTable() {
        TableColumn<Customer, String> idColumn = new TableColumn<>("Id");
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        TableColumn<Customer, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName())
        );

        TableColumn<Customer, String> phoneColumn = new TableColumn<>("Phone number");
        phoneColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone())
        );

        TableColumn<Customer, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEmail())
        );

        TableColumn<Customer, String> vipColumn = new TableColumn<>("VIP");
        vipColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isVip() ? "Yes" : "No")
        );

        customerTable.getColumns().addAll(idColumn, nameColumn, phoneColumn, emailColumn, vipColumn);
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        customerTable.setItems(filteredData);
    }

    private void configureSearch() {
        searchField.setPromptText("Search by name or phone number...");
        searchField.getStyleClass().add("input");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter(newValue));
    }

    private void applyFilter(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase();
        filteredData.setPredicate(customer -> {
            if (query.isEmpty()) {
                return true;
            }

            return customer.getName().toLowerCase().contains(query)
                    || customer.getPhone().toLowerCase().contains(query);
        });
    }

    private HBox createToolbar() {
        Button newCustomerButton = new Button("New Customer");
        newCustomerButton.getStyleClass().addAll("btn", "btn-primary");
        newCustomerButton.setOnAction(event -> openCreateCustomerDialog());

        HBox toolbar = new HBox(15, newCustomerButton, searchField);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        return toolbar;
    }

    private void openCreateCustomerDialog() {
        CreateCustomerDialog dialog = new CreateCustomerDialog();
        if (dialog.showAndWait()) {
            refreshData();
        }
    }

}