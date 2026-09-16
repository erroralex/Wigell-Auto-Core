package com.wac.autocore.view.util;

import javafx.scene.control.DialogPane;

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
}
