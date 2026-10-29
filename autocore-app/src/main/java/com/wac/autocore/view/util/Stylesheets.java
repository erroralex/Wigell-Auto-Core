package com.wac.autocore.view.util;

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.net.URL;
import java.util.List;

/**
 * <b>Stylesheets</b>
 * <p>Responsibility: The single place that knows which CSS files make up the app's theme
 * and in which order they are loaded. Used by the main scene, the login view and all dialogs,
 * so a new theme file only has to be registered here.</p>
 */
public final class Stylesheets {

    private static final String BASE_PATH = "/com/wac/autocore/view/";

    private static final List<String> THEME = List.of(
            "theme.css",
            "style.css"

    );

    private Stylesheets() {
    }

    public static void applyTo(Scene scene) {
        addMissing(scene.getStylesheets(), THEME);
    }

    public static void applyTo(Parent parent) {
        addMissing(parent.getStylesheets(), THEME);
    }

    public static void addViewStylesheet(Parent parent, String fileName) {
        addMissing(parent.getStylesheets(), List.of(fileName));
    }

    static List<String> themeUrls() {
        return THEME.stream().map(Stylesheets::resolve).toList();
    }

    private static void addMissing(ObservableList<String> target, List<String> fileNames) {
        for (String fileName : fileNames) {
            String url = resolve(fileName);
            if (!target.contains(url)) {
                target.add(url);
            }
        }
    }

    private static String resolve(String fileName) {
        URL url = Stylesheets.class.getResource(BASE_PATH + fileName);
        if (url == null) {
            throw new IllegalStateException("Stylesheet not found on classpath: " + BASE_PATH + fileName);
        }
        return url.toExternalForm();
    }
}
