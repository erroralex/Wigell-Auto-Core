package com.wac.autocore.view.dialog;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class AdminServiceItemDialog extends Dialog<ServiceItem> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final ServiceItemService serviceItemService;
    private final ServiceItem serviceItem;
    private ServiceItem savedItem;

    private final TextField priceTextField = new TextField();
    private final Label errorLabel = new Label();

    private final ButtonType saveButtonType =
            new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);

    private final ButtonType cancelButtonType =
            new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public AdminServiceItemDialog(ServiceItemService serviceItemService, ServiceItem serviceItem) {
        this.serviceItemService = serviceItemService;
        this.serviceItem = serviceItem;

        setTitle(lang.get("serviceItem.editPrice"));
        setHeaderText(serviceItem.getName());
        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(
                saveButtonType,
                cancelButtonType
        );

        errorLabel.getStyleClass().add("text-error");

        priceTextField.setText(DialogUtil.formatMoney(serviceItem.getPrice()));

        setContent();
        handleInput();
    }

    private void setContent() {
        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        content.getChildren().addAll(
                errorLabel,
                new Label(lang.get("table.price")),
                priceTextField
        );

        getDialogPane().setContent(content);
    }

    private void handleInput() {
        Button saveButton =
                (Button) getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {

            try {
                Money newPrice = DialogUtil.parseMoney(priceTextField.getText());
                savedItem = serviceItemService.updatePrice(serviceItem.getId(), newPrice);
            } catch (ValidationException e) {
                rejectPrice(event, e.getMessageKey());
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return savedItem;
            }
            return null;
        });
    }

    // Visar felmeddelandet och hindrar dialogen från att stängas
    private void rejectPrice(ActionEvent event, String messageKey) {
        errorLabel.setText(lang.get(messageKey));
        event.consume();
    }
}
