package com.wac.autocore.view.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * <b>AlertHelper</b>
 * <p>Ansvar: Hjälpklass för att visa dialogrutor och varningar i användargränssnittet.</p>
 * <p><b>Exempel på användning:</b></p>
 * <pre>{@code
 * if (nameField.getText().isEmpty()) {
 *     AlertHelper.showError("Valideringsfel", "Namn måste anges.");
 *     return;
 * }
 * }</pre>
 */
public class AlertHelper {
    private AlertHelper() {
    }

    // Visar ett enkelt felmeddelande med OK-knapp
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Visar ett informationsmeddelande med OK-knapp
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Visar en bekräftelsedialog med OK/Avbryt och returnerar true om användaren bekräftade
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
