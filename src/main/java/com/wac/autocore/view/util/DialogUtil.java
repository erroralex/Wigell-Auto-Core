package com.wac.autocore.view.util;

import com.wac.autocore.view.AutoCoreApp;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.stage.Screen;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

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
        DialogPane pane = dialog.getDialogPane();
        dialog.setResizable(true);

        // Gör innehållsdelen scrollbar när den inte får plats i dialogfönstret.
        pane.contentProperty().addListener((observable, oldContent, content) -> {
            if (content == null || content instanceof ScrollPane) {
                return;
            }

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setMinHeight(0);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(false);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            pane.setContent(scrollPane);
        });

        double[] zoom = {1.0};

        // Zooma dialogens innehåll med Ctrl + mushjul.
        pane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (!event.isControlDown() || event.getDeltaY() == 0) {
                return;
            }

            double step = 1.05;
            zoom[0] = Math.max(0.7, Math.min(3.0,
                    zoom[0] * (event.getDeltaY() > 0 ? step : 1 / step)));
            pane.setStyle("-fx-font-size: " + (13 * zoom[0]) + "px;");

            Platform.runLater(() -> resizeDialog(pane));

            event.consume();
        });

        dialog.setOnShown(event -> Platform.runLater(() -> {

            resizeDialog(pane);

            // Centrerar dialogen över dess ägare.
            if (pane.getScene().getWindow() instanceof Stage) {
                Stage stage = (Stage) pane.getScene().getWindow();

                if (stage.getOwner() != null) {
                    javafx.stage.Window owner = stage.getOwner();

                    stage.setX(
                            owner.getX() + (owner.getWidth() - stage.getWidth()) / 2
                    );
                    stage.setY(
                            owner.getY() + (owner.getHeight() - stage.getHeight()) / 2
                    );

                    // Behåll den centrerade dialogen inom det tillåtna området.
                    resizeDialog(pane);
                }
            }
        }));

        // Sätter dialogens ägare till huvudfönstret.
        Stage owner = AutoCoreApp.getPrimaryStage();
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
    }

    // Justerar dialogens storlek så att den får plats på skärmen och lägger till en scroll för innehållet om det inte får plats.
    private static void resizeDialog(DialogPane pane) {
        if (pane.getScene() == null
                || !(pane.getScene().getWindow() instanceof Stage)
                || !(pane.getContent() instanceof ScrollPane)) {
            return;
        }

        Stage stage = (Stage) pane.getScene().getWindow();
        ScrollPane scrollPane = (ScrollPane) pane.getContent();

        Rectangle2D bounds = Screen.getScreensForRectangle(
                stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()
        ).get(0).getVisualBounds();

        // Om dialogen har en ägare, begränsa dialogens storlek till ägarens storlek.
        if (stage.getOwner() != null) {
            javafx.stage.Window owner = stage.getOwner();

            double left = Math.max(bounds.getMinX(), owner.getX());
            double top = Math.max(bounds.getMinY(), owner.getY());
            double right = Math.min(
                    bounds.getMaxX(), owner.getX() + owner.getWidth()
            );
            double bottom = Math.min(
                    bounds.getMaxY(), owner.getY() + owner.getHeight()
            );

            if (right > left && bottom > top) {
                bounds = new Rectangle2D(left, top, right - left, bottom - top);
            }
        }

        for (ButtonType buttonType : pane.getButtonTypes()) {
            Node button = pane.lookupButton(buttonType);

            if (button instanceof Region) {
                Region region = (Region) button;

                // Knapparna får plats med sin text vid aktuell zoom.
                ButtonBar.setButtonUniformSize(button, false);
                region.setPrefWidth(Region.USE_COMPUTED_SIZE);
                region.setMinWidth(Region.USE_PREF_SIZE);
                region.setMinHeight(Region.USE_PREF_SIZE);
            }
        }

        scrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);

        pane.applyCss();
        pane.layout();

        double windowBorders = stage.getHeight() - pane.getScene().getHeight();
        double maxWindowHeight = bounds.getHeight() - 16;
        double width = pane.getWidth();

        // Det av dialogen som ligger utanför scrollpanelen (rubrik, knapprad och mellanrum).
        double fixedHeight = Math.max(
                0,
                pane.prefHeight(width) - scrollPane.prefHeight(width)
        );

        double availableHeight = Math.max(
                0,
                maxWindowHeight - windowBorders - fixedHeight
        );

        double contentHeight = scrollPane.prefHeight(width);

        scrollPane.setPrefHeight(Math.min(contentHeight, availableHeight));
        scrollPane.setMaxHeight(availableHeight);

        stage.setMaxHeight(maxWindowHeight);
        stage.setMaxWidth(bounds.getWidth());
        stage.sizeToScene();

        if (stage.getHeight() > maxWindowHeight) {
            stage.setHeight(maxWindowHeight);
        }

        if (stage.getWidth() > bounds.getWidth()) {
            stage.setWidth(bounds.getWidth());
        }

        stage.setX(Math.max(
                bounds.getMinX(),
                Math.min(stage.getX(), bounds.getMaxX() - stage.getWidth())
        ));

        // Håller dialogens nederkant inom skärmen.
        stage.setY(Math.max(
                bounds.getMinY(),
                Math.min(stage.getY(), bounds.getMaxY() - stage.getHeight())
        ));
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
