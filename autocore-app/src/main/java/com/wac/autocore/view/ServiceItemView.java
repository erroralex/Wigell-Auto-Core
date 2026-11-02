package com.wac.autocore.view;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.security.Permission;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.view.dialog.AdminServiceItemDialog;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import com.wac.autocore.view.util.ViewPermissionUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


/**
 * <b>ServiceItemView</b>
 * <p>Responsibility: Shows and manages service items in the user interface.</p>
 */
public class ServiceItemView extends BaseView {

    private static final LanguageManager lang = LanguageManager.getInstance();
    private final ServiceItemService serviceItemService;

    private final Button btnEditPrice = new Button(lang.get("serviceItem.editPrice"));
    private final TableView<ServiceItem> itemTableView = new TableView<>();
    private final ObservableList<ServiceItem> serviceItemList;

    public ServiceItemView(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
        this.serviceItemList = FXCollections.observableArrayList(serviceItemService.listAll());

        initializeTable();
        initView();
    }

    @Override
    protected String getTitleKey() {
        return "serviceItem.title";
    }

    @Override
    protected void buildContent() {
        VBox.setVgrow(itemTableView, Priority.ALWAYS);
        getChildren().addAll(createButtonBar(itemTableView), itemTableView);
    }

    private void initializeTable() {
        itemTableView.setEditable(false);
        itemTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemTableView.setPlaceholder(new Label(lang.get("table.empty")));

        VBox.setVgrow(itemTableView, Priority.ALWAYS);

        TableColumn<ServiceItem, String> nameColumn = new TableColumn<>(lang.get("table.name"));
        TableColumn<ServiceItem, String> descColumn = new TableColumn<>(lang.get("table.desc"));
        TableColumn<ServiceItem, Money> priceColumn = new TableColumn<>(lang.get("table.price"));
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
    }

    private HBox createButtonBar(TableView<ServiceItem> itemTableView) {
        String btnPrimary = "btn-primary";

        btnEditPrice.getStyleClass().addAll("btn", btnPrimary);

        btnEditPrice.setOnAction(event ->
                openAdminServiceItemDialog(itemTableView));
        ViewPermissionUtil.hideUnless(Permission.SERVICE_CATALOG_WRITE, btnEditPrice);

        HBox toolbar = new HBox(btnEditPrice);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private void setPriceColumn(TableColumn<ServiceItem, Money> priceColumn) {
        priceColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getPrice()));

        DialogUtil.configureMoneyColumn(priceColumn);
    }

    private void openAdminServiceItemDialog(TableView<ServiceItem> itemTableView) {

        ServiceItem selected = itemTableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return;
        }

        AdminServiceItemDialog dialog =
                new AdminServiceItemDialog(serviceItemService, selected);

        dialog.showAndWait().ifPresent(saved ->
                serviceItemList.setAll(serviceItemService.listAll()));
    }
}
