package com.wac.autocore.view.util;

import com.wac.autocore.view.AutoCoreApp;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

/**
 * <b>DialogUtil</b>
 * <p>Ansvar: Gemensamma hjälpmetoder för dialoger och alerts, t.ex. att applicera
 * appens tema på en DialogPane. Dialog/Alert ärver inte automatiskt stilmallen från
 * huvudscenen, så denna metod måste anropas explicit av varje dialog.</p>
 */
public class DialogUtil {

    private static final String STYLESHEET_PATH = "/com/wac/autocore/view/style.css";

    private DialogUtil() {
    }

    // Applicerar appens tema på given DialogPane
    public static void applyTheme(DialogPane pane) {
        pane.getStylesheets().add(
                DialogUtil.class.getResource(STYLESHEET_PATH).toExternalForm()
        );
    }

    // Applicerar appens tema på given Dialog och sätter _ägare till huvudfönstret_ om det inte redan är satt
    public static void applyTheme(Dialog<?> dialog) {
        applyTheme(dialog.getDialogPane());

        Stage owner = AutoCoreApp.getPrimaryStage();
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
    }

    // Applicerar appens tema på given Alert och sätter _ägare till huvudfönstret_ om det inte redan är satt
    public static void applyTheme(Alert alert) {
        applyTheme(alert.getDialogPane());

        Stage owner = AutoCoreApp.getPrimaryStage();
        if (owner != null && alert.getOwner() == null) {
            alert.initOwner(owner);
        }
    }

}
