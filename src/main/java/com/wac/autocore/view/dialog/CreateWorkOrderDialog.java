package com.wac.autocore.view.dialog;

import com.wac.autocore.model.Booking;
import com.wac.autocore.service.LanguageManager;
import com.wac.autocore.view.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.List;

/**
 * <b>CreateWorkOrderDialog</b>
 * <p>Ansvar: Dialog för att skapa en arbetsorder. Användaren väljer endast en bokning;
 * mekaniker och tjänster ärvs från bokningen i WorkOrderService.
 * Får sin lista av vyn och känner inte till databasen.</p>
 */
public class CreateWorkOrderDialog extends Dialog<Booking> {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private final ComboBox<Booking> bookingCombo = new ComboBox<>();

    private final ButtonType saveButtonType = new ButtonType(lang.get("btn.save"), ButtonBar.ButtonData.OK_DONE);
    private final ButtonType cancelButtonType = new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

    public CreateWorkOrderDialog(List<Booking> bookings) {
        setTitle(lang.get("workOrder.new"));
        setHeaderText(lang.get("workOrder.create"));

        // Hämta css-styling och applicera på nya dialog:
        DialogUtil.applyTheme(this);

        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        bookingCombo.getItems().addAll(bookings);
        bookingCombo.setMaxWidth(Double.MAX_VALUE);
        bookingCombo.setConverter(new StringConverter<Booking>() {

            @Override
            public String toString(Booking booking) {
                return booking == null ? "" : "#" + booking.getId() + " - " + booking.getDescription();
            }

            @Override
            public Booking fromString(String string) {
                return null;
            }
        });

        Node saveButton = getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(bookingCombo.valueProperty().isNull());

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));
        content.getChildren().addAll(
                new Label(lang.get("table.booking")), bookingCombo
        );
        getDialogPane().setContent(content);

        setResultConverter(buttonType ->
                buttonType == saveButtonType ? bookingCombo.getValue() : null
        );
    }
}
