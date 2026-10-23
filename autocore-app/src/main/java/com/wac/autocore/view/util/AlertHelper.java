package com.wac.autocore.view.util;

import com.wac.autocore.view.i18n.LanguageManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * <b>AlertHelper</b>
 * <p>Responsibility: Helper class for showing dialogs and warnings in the user interface.</p>
 * <p><b>Example usage:</b></p>
 * <pre>{@code
LanguageManager lang = LanguageManager.getInstance();
 * if (nameField.getText().isEmpty()) {
 *     AlertHelper.showError(lang.get("error.validation"), lang.get("error.requiredField", lang.get("table.name")));
 *     return;
 * }
 * }</pre>
 */
public class AlertHelper {

    private static final LanguageManager lang = LanguageManager.getInstance();

    private AlertHelper() {
    }


    // Shows a simple error message with an OK button
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "", okButton());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogUtil.applyTheme(alert);
        alert.showAndWait();
    }

    // Shows an information message with an OK button
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", okButton());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogUtil.applyTheme(alert);
        alert.showAndWait();
    }

    // Shows a confirmation dialog with OK/Cancel and returns true if the user confirmed
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", okButton(), cancelButton());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogUtil.applyTheme(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    // Shows a warning with two custom choices and returns true if the user chose the first one
    public static boolean showWarningChoice(String title, String message, String acceptText, String declineText) {
        ButtonType accept = new ButtonType(acceptText, ButtonBar.ButtonData.OK_DONE);
        ButtonType decline = new ButtonType(declineText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.WARNING, "", accept, decline);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogUtil.applyTheme(alert);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == accept;
    }

    // Shows an error message with an expandable stack trace, for unexpected errors
    public static void showException(String title, String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, "", okButton());
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(e.toString());
        DialogUtil.applyTheme(alert);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();

        TextArea textArea = new TextArea(stackTrace);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(textArea, 0, 0);

        // Hidden by default behind the "Show Details" button (Alert's built-in behavior)
        alert.getDialogPane().setExpandableContent(content);

        alert.showAndWait();
    }

    private static ButtonType okButton() {
        return new ButtonType(lang.get("btn.ok"), ButtonBar.ButtonData.OK_DONE);
    }

    private static ButtonType cancelButton() {
        return new ButtonType(lang.get("btn.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
    }
}
