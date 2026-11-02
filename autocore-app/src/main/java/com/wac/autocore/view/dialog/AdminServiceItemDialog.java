package com.wac.autocore.view.dialog;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.view.util.DialogUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * <b>AdminServiceItemDialog</b>
 * <p>Responsibility: Dialog for changing the price of a service item.</p>
 *
 * <p>Concrete subclass in the Template Method pattern: {@link FormDialog} builds the dialog,
 * and this class supplies only the content, the save action and the result.</p>
 */
public class AdminServiceItemDialog extends FormDialog<ServiceItem> {

    private final ServiceItemService serviceItemService;
    private final ServiceItem serviceItem;
    private ServiceItem savedItem;

    private final TextField priceTextField = new TextField();

    public AdminServiceItemDialog(ServiceItemService serviceItemService, ServiceItem serviceItem) {
        this.serviceItemService = serviceItemService;
        this.serviceItem = serviceItem;

        priceTextField.setText(DialogUtil.formatMoney(serviceItem.getPrice()));

        initDialog();
    }

    @Override
    protected String getTitleKey() {
        return "serviceItem.editPrice";
    }

    @Override
    protected String buildHeaderText() {
        return serviceItem.getName();
    }

    @Override
    protected Node buildContent() {
        VBox content = new VBox();
        content.getChildren().addAll(
                new Label(lang.get("table.price")),
                priceTextField
        );
        return content;
    }

    @Override
    protected void submit() {
        Money newPrice = DialogUtil.parseMoney(priceTextField.getText());
        savedItem = serviceItemService.updatePrice(serviceItem.getId(), newPrice);
    }

    @Override
    protected ServiceItem buildResult() {
        return savedItem;
    }
}
