package com.wac.autocore.view.util;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;

import java.util.Locale;

/**
 * <b>ZoomManager</b>
 * <p>Responsibility: One zoom level for the whole app. Ctrl+scroll, Ctrl+plus, Ctrl+minus and Ctrl+0
 * change it, and the main window, dialogs and alerts all follow. Every size in the CSS is in em,
 * so changing the root font size scales text, spacing and controls together.</p>
 * <p>Singleton like LanguageManager. There is one zoom level, shared by every window.</p>
 */
public final class ZoomManager {

    // Base font size in px at 100 % zoom. Must match -fx-font-size in .root (style.css).
    public static final double BASE_FONT_SIZE = 19;

    private static final double MIN_ZOOM = 0.7;
    private static final double MAX_ZOOM = 3.0;
    private static final double STEP = 1.05;

    private static final String INSTALLED_KEY = "zoomManager.installed";

    private static final ZoomManager INSTANCE = new ZoomManager();

    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);

    private final StringBinding fontSizeStyle = Bindings.createStringBinding(
            () -> String.format(Locale.ROOT, "-fx-font-size: %.2fpx;", BASE_FONT_SIZE * zoom.get()),
            zoom);

    private ZoomManager() {
    }

    public static ZoomManager getInstance() {
        return INSTANCE;
    }

    public ReadOnlyDoubleProperty zoomProperty() {
        return zoom;
    }

    public void zoomIn() {
        setZoom(zoom.get() * STEP);
    }

    public void zoomOut() {
        setZoom(zoom.get() / STEP);
    }

    public void reset() {
        zoom.set(1.0);
    }

    public void install(Scene scene) {
        scene.addEventFilter(ScrollEvent.SCROLL, this::onScroll);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);

        bindFontSize(scene.getRoot());
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (oldRoot != null) {
                oldRoot.styleProperty().unbind();
            }
            bindFontSize(newRoot);
        });
    }

    public void install(Parent pane) {
        if (pane.getProperties().containsKey(INSTALLED_KEY)) {
            return;
        }
        pane.getProperties().put(INSTALLED_KEY, Boolean.TRUE);

        pane.addEventFilter(ScrollEvent.SCROLL, this::onScroll);
        pane.addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        bindFontSize(pane);
    }

    private void setZoom(double value) {
        zoom.set(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value)));
    }

    private void bindFontSize(Parent root) {
        if (root != null) {
            root.styleProperty().bind(fontSizeStyle);
        }
    }

    // isShortcutDown is Ctrl on Windows and Cmd on macOS
    private void onScroll(ScrollEvent event) {
        if (!event.isShortcutDown() || event.getDeltaY() == 0) {
            return;
        }
        if (event.getDeltaY() > 0) {
            zoomIn();
        } else {
            zoomOut();
        }
        event.consume();
    }

    private void onKeyPressed(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        switch (event.getCode()) {
            case PLUS:
            case EQUALS:
            case ADD:
                zoomIn();
                break;
            case MINUS:
            case SUBTRACT:
                zoomOut();
                break;
            case DIGIT0:
            case NUMPAD0:
                reset();
                break;
            default:
                return;
        }
        event.consume();
    }
}
