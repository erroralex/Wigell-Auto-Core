package com.wac.autocore.view.util;

import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.MoneyFormat;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.AutoCoreApp;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Screen;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * <b>DialogUtil</b>
 * <p>Responsibility: Common helper methods for dialogs and alerts, e.g. applying
 * the app's theme to a DialogPane. Dialog/Alert do not automatically inherit the stylesheet from
 * the main scene, so this method must be called explicitly by each dialog.</p>
 */
public class DialogUtil {

    private static final String STYLESHEET_PATH = "/com/wac/autocore/view/style.css";

    private DialogUtil() {
    }

    // Applies the app's theme to the given DialogPane
    public static void applyTheme(DialogPane pane) {
        pane.getStylesheets().add(
                DialogUtil.class.getResource(STYLESHEET_PATH).toExternalForm()
        );
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
        });

        double[] zoom = {1.0};

        // Zoom the dialog's content with Ctrl + mouse wheel.
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

    // Applies the app's theme to the given Alert and sets the _owner to the main window_ if it is not already set
    public static void applyTheme(Alert alert) {
        applyTheme(alert.getDialogPane());

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
