package com.wac.autocore.view.util;

import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.MoneyFormat;
import com.wac.autocore.view.AutoCoreApp;
import com.wac.autocore.view.i18n.LanguageManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * <b>DialogUtil</b>
 * <p>Responsibility: Common helper methods for dialogs and alerts, e.g. applying
 * the app's theme to a DialogPane. Dialog/Alert do not automatically inherit the stylesheet from
 * the main scene, so this method must be called explicitly by each dialog.</p>
 */
public class DialogUtil {

    private DialogUtil() {
    }

    // Applies the app's theme and the app-wide zoom to the given DialogPane
    public static void applyTheme(DialogPane pane) {
        Stylesheets.applyTo(pane);
        ZoomManager.getInstance().install(pane);
    }

    // Applies the app's theme to the given Dialog and sets the _owner to the main window_ if it is not already set
    public static void applyTheme(Dialog<?> dialog) {
        applyTheme(dialog.getDialogPane());
        DialogPane pane = dialog.getDialogPane();
        dialog.setResizable(true);

        // Makes the content area scrollable when it does not fit in the dialog window.
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

            // Refits the dialog when its content changes height.
            // Only the height is compared, since resizing changes the width and would otherwise trigger again.
            content.layoutBoundsProperty().addListener((boundsObservable, oldBounds, newBounds) -> {
                if (dialog.isShowing() && oldBounds.getHeight() != newBounds.getHeight()) {
                    Platform.runLater(() -> resizeDialog(pane));
                }
            });
        });

        ChangeListener<Number> refitOnZoom = (obs, oldZoom, newZoom) ->
                Platform.runLater(() -> resizeDialog(pane));
        dialog.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                ZoomManager.getInstance().zoomProperty().addListener(refitOnZoom);
            } else {
                ZoomManager.getInstance().zoomProperty().removeListener(refitOnZoom);
            }
        });

        dialog.setOnShown(event -> Platform.runLater(() -> {

            resizeDialog(pane);

            // Centers the dialog over its owner.
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

                    // Keep the centered dialog within the allowed area.
                    resizeDialog(pane);
                }
            }
        }));

        // Sets the dialog's owner to the main window.
        Stage owner = AutoCoreApp.getPrimaryStage();
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
        }
    }

    // Adjusts the dialog's size so it fits on the screen and adds a scroll for the content if it does not fit.
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

        // If the dialog has an owner, limit the dialog's size to the owner's size.
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

                // The buttons fit with their text at the current zoom.
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

        // The part of the dialog outside the scroll pane (header, button bar and spacing).
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

        // Keeps the dialog's bottom edge within the screen.
        stage.setY(Math.max(
                bounds.getMinY(),
                Math.min(stage.getY(), bounds.getMaxY() - stage.getHeight())
        ));
    }

    public static void applyTheme(Alert alert) {
        applyTheme(alert.getDialogPane());
        DialogPane pane = alert.getDialogPane();

        // The pane may never be smaller than its content.
        pane.setMinWidth(Region.USE_PREF_SIZE);
        pane.setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);

        for (ButtonType type : alert.getButtonTypes()) {
            Node button = pane.lookupButton(type);
            if (button instanceof Button) {
                ButtonBar.setButtonUniformSize(button, false);
                ((Button) button).setMinWidth(Region.USE_PREF_SIZE);
            }
        }

        // Grow or shrink the window when the zoom changes while the alert is open
        ChangeListener<Number> refitOnZoom = (obs, oldZoom, newZoom) ->
                Platform.runLater(() -> pane.getScene().getWindow().sizeToScene());
        alert.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                ZoomManager.getInstance().zoomProperty().addListener(refitOnZoom);
            } else {
                ZoomManager.getInstance().zoomProperty().removeListener(refitOnZoom);
            }
        });

        Stage owner = AutoCoreApp.getPrimaryStage();
        if (owner != null && alert.getOwner() == null) {
            alert.initOwner(owner);
        }
    }

    // Formats an amount with two decimals according to the current language, without thousands separator
    public static String formatMoney(Money money) {
        return formatMoney(money, MoneyFormat.Style.PLAIN);
    }

    public static String formatMoney(Money money, MoneyFormat.Style style) {
        return MoneyFormat.format(money, LanguageManager.getInstance().getLocale(), style);
    }

    // Parses an entered amount, both comma and period are accepted
    public static Money parseMoney(String text) {
        return MoneyFormat.parse(text);
    }

    // Factory so the table does not show the Money object's toString(), but formatted text.
    // formatMoney(item, GROUPED) uses the locale and two decimals.
    public static <S> void configureMoneyColumn(TableColumn<S, Money> col) {
        configureMoneyColumn(col, false);
    }

    // With withCurrency the amount is shown as on the invoice, e.g. 1097,50 kr. The currency text comes from the language bundle.
    public static <S> void configureMoneyColumn(TableColumn<S, Money> col, boolean withCurrency) {
        col.setCellFactory(c -> new TableCell<S, Money>() {
            {
                setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            protected void updateItem(Money item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else if (withCurrency) {
                    setText(LanguageManager.getInstance().get("format.price", item));
                } else {
                    setText(formatMoney(item, MoneyFormat.Style.GROUPED));
                }
            }
        });
    }

}
