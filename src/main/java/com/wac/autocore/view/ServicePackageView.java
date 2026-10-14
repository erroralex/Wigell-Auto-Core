package com.wac.autocore.view;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.view.dialog.ServicePackageDialog;
import com.wac.autocore.view.util.AlertHelper;
import com.wac.autocore.view.util.ErrorFacade;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <b>ServicePackageView</b>
 * <p>Ansvar: Admin-vy för att lista, skapa, redigera och ta bort servicepaket.</p>
 * <p>Totalpriset är summan av dagens katalogpriser. Paketet är bara en mall; en bokning
 * fryser sina egna priser, så det som visas här påverkar aldrig befintliga bokningar.</p>
 */
public class ServicePackageView extends BaseView {

    private final ServicePackageService servicePackageService;
    private final ServiceItemService serviceItemService;

    private final TableView<ServicePackage> table = new TableView<>();
    private final ObservableList<ServicePackage> packages = FXCollections.observableArrayList();

    public ServicePackageView(ServicePackageService servicePackageService, ServiceItemService serviceItemService) {
        this.servicePackageService = servicePackageService;
        this.serviceItemService = serviceItemService;

        initializeTable();
        refresh();
        initView();
    }

    @Override
    protected String getTitleKey() {
        return "servicePackage.title";
    }

    @Override
    protected void buildContent() {
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(createButtonBar(), table);
    }

    private void initializeTable() {
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(lang.get("table.empty")));

        TableColumn<ServicePackage, String> nameColumn = new TableColumn<>(lang.get("table.name"));
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getName()));

        TableColumn<ServicePackage, String> descColumn = new TableColumn<>(lang.get("table.desc"));
        descColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDescription()));

        TableColumn<ServicePackage, Integer> countColumn = new TableColumn<>(lang.get("table.serviceCount"));
        countColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(cell.getValue().getServices().size()));

        TableColumn<ServicePackage, Money> priceColumn = new TableColumn<>(lang.get("table.totalPrice"));
        priceColumn.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(totalPrice(cell.getValue())));

        setPriceFormat(priceColumn);

        table.getColumns().add(nameColumn);
        table.getColumns().add(descColumn);
        table.getColumns().add(countColumn);
        table.getColumns().add(priceColumn);

        table.setItems(packages);
    }

    // Följer valt språk: 1 097,50 på svenska, 1,097.50 på engelska
    private void setPriceFormat(TableColumn<ServicePackage, Money> priceColumn) {
        NumberFormat formatter = NumberFormat.getNumberInstance(lang.getLocale());
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        priceColumn.setCellFactory(column -> new TableCell<ServicePackage, Money>() {
            @Override
            protected void updateItem(Money price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : formatter.format(price.toBigDecimal()));
            }
        });
    }

    private static Money totalPrice(ServicePackage servicePackage) {
        Money sum = Money.ZERO;
        for (ServiceItem service : servicePackage.getServices()) {
            sum = sum.add(service.getPrice());
        }
        return sum;
    }

    // Sorteras här i vyn, så att ServicePackageService inte behöver ändras
    private void refresh() {
        List<ServicePackage> all = new ArrayList<>(servicePackageService.findAll());
        all.sort(Comparator.comparing(ServicePackage::getName, String.CASE_INSENSITIVE_ORDER));
        packages.setAll(all);
    }

    private HBox createButtonBar() {
        Button createButton = new Button(lang.get("btn.createNew"));
        Button editButton = new Button(lang.get("btn.edit"));
        Button deleteButton = new Button(lang.get("btn.remove"));

        createButton.getStyleClass().addAll("btn", "btn-primary");
        editButton.getStyleClass().addAll("btn", "btn-secondary");
        deleteButton.getStyleClass().addAll("btn", "btn-secondary");

        // Redigera och Ta bort kräver en markerad rad
        BooleanBinding noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        createButton.setOnAction(event -> openDialog(
                ServicePackageDialog.forCreate(servicePackageService, serviceItemService.listAll())));
        editButton.setOnAction(event -> openDialog(
                ServicePackageDialog.forEdit(servicePackageService, serviceItemService.listAll(), selectedPackage())));
        deleteButton.setOnAction(event -> deleteSelected());

        HBox bar = new HBox(15, createButton, editButton, deleteButton);
        bar.setPadding(new Insets(15, 0, 0, 0));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ServicePackage selectedPackage() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void openDialog(ServicePackageDialog dialog) {
        dialog.showAndWait().ifPresent(saved -> refresh());
    }

    private void deleteSelected() {
        ServicePackage selected = selectedPackage();
        if (selected == null) {
            return;
        }

        boolean confirmed = AlertHelper.showConfirmation(
                lang.get("servicePackage.deleteConfirmTitle"),
                lang.get("servicePackage.deleteConfirmMsg", selected.getName()));
        if (!confirmed) {
            return;
        }

        try {
            servicePackageService.delete(selected.getId());
            refresh();
        } catch (RuntimeException e) {
            ErrorFacade.handle(e);
        }
    }
}
