package com.wac.autocore.view.dialog;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.view.util.DialogUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.IllformedLocaleException;

public class AdminServiceItemDialog extends Dialog<Double> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final ServiceItemService serviceItemService;
    private final ServiceItem serviceItem;

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

        priceTextField.setText(String.valueOf(serviceItem.getPrice()));

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
                double newPrice = Double.parseDouble(
                        priceTextField.getText().replace(',', '.')
                );

                if (newPrice <= 0) {
                    throw new IllegalArgumentException(); // TODO: CHANGE TO CUSTOM EXCEPTION
                }
            } catch (Exception e) {
                errorLabel.setText(lang.get("error.serviceItem.invalidPrice"));
                event.consume();
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                return Double.parseDouble(
                        priceTextField.getText().replace(',', '.')
                );
            }
            return null;
        });
    }
}
