package com.wac.autocore.view;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.view.dialog.CreateVehicleDialog;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * <b>VehicleView</b>
 * <p>Ansvar: Visar och hanterar fordon i användargränssnittet.</p>
 */
public class VehicleView extends VBox {

    private static final Customer ALL_CUSTOMERS = new Customer(0, "All Customers", "", "");

    private final TableView<Vehicle> vehicleTable = new TableView<>();
    private final ObservableList<Vehicle> masterData = FXCollections.observableArrayList();
    private final FilteredList<Vehicle> filteredData = new FilteredList<>(masterData, vehicle -> true);
    private final ObservableList<Customer> customerFilterOptions = FXCollections.observableArrayList();
    private final ComboBox<Customer> customerFilter = new ComboBox<>();
    private final Map<Integer, String> customerNamesById = new HashMap<>();

    public VehicleView() {
        this.getStyleClass().add("content-area");
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(vehicleTable, Priority.ALWAYS);

        Label title = new Label("Vehicles");
        title.getStyleClass().add("text-title");

        loadCustomers();
        loadMasterData();
        initializeTable();
        configureCustomerFilter();

        this.getChildren().addAll(title, createToolbar(), vehicleTable);
    }

    private void loadCustomers() {
        customerNamesById.clear();
        customerFilterOptions.setAll(Database.getCustomers());

        for (Customer customer : Database.getCustomers()) {
            customerNamesById.put(customer.getId(), customer.getName());
        }
    }

    private void loadMasterData() {
        masterData.setAll(Database.getVehicles());
    }

    private void refreshData() {
        loadCustomers();
        masterData.setAll(Database.getVehicles());
        applyCustomerFilter(customerFilter.getValue());
    }

    private void initializeTable() {
        TableColumn<Vehicle, String> idColumn = new TableColumn<>("Id");
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getId()))
        );

        TableColumn<Vehicle, String> registrationNumberColumn = new TableColumn<>("Registration Number");
        registrationNumberColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getRegistrationNumber())
        );

        TableColumn<Vehicle, String> makeColumn = new TableColumn<>("Make");
        makeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getBrand())
        );

        TableColumn<Vehicle, String> modelColumn = new TableColumn<>("Model");
        modelColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getModel())
        );

        TableColumn<Vehicle, String> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getYear()))
        );

        TableColumn<Vehicle, String> ownerColumn = new TableColumn<>("Owner");
        ownerColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(resolveOwnerName(cellData.getValue().getCustomerId()))
        );

        vehicleTable.getColumns().addAll(
                idColumn,
                registrationNumberColumn,
                makeColumn,
                modelColumn,
                yearColumn,
                ownerColumn
        );
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vehicleTable.setItems(filteredData);
    }

    private void configureCustomerFilter() {
        customerFilter.getStyleClass().add("combo-box");
        customerFilter.setPromptText("Filter by Customer");
        customerFilter.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String string) {
                return null;
            }
        });

        customerFilterOptions.add(0, ALL_CUSTOMERS);
        customerFilter.setItems(customerFilterOptions);
        customerFilter.setValue(ALL_CUSTOMERS);
        customerFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyCustomerFilter(newValue));
    }

    private void applyCustomerFilter(Customer selectedCustomer) {
        filteredData.setPredicate(vehicle -> {
            if (selectedCustomer == null || selectedCustomer.getId() == ALL_CUSTOMERS.getId()) {
                return true;
            }

            return vehicle.getCustomerId() == selectedCustomer.getId();
        });
    }

    private String resolveOwnerName(int customerId) {
        String customerName = customerNamesById.get(customerId);
        return customerName == null ? "" : customerName;
    }

    private HBox createToolbar() {
        Button newVehicleButton = new Button("New Vehicle");
        newVehicleButton.getStyleClass().addAll("btn", "btn-primary");
        newVehicleButton.setOnAction(event -> openCreateVehicleDialog());

        HBox toolbar = new HBox(15, newVehicleButton, customerFilter);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(customerFilter, Priority.ALWAYS);
        return toolbar;
    }

    private void openCreateVehicleDialog() {
        CreateVehicleDialog dialog = new CreateVehicleDialog();
        if (dialog.showAndWait()) {
            refreshData();
        }
    }

}