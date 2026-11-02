package com.wac.autocore.view.component;

import com.wac.autocore.view.i18n.LanguageManager;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * <b>QuickLink</b>
 * <p>Responsibility: A card-shaped button with a title and a short description, used for shortcuts on the home view.</p>
 */
public class QuickLink extends Button {

    private final LanguageManager lang = LanguageManager.getInstance();

    public QuickLink(String titleKey, String textKey, Runnable onOpen) {
        Label title = new Label();
        title.textProperty().bind(lang.bind(titleKey));
        title.getStyleClass().add("quick-link-title");

        Label text = new Label();
        text.textProperty().bind(lang.bind(textKey));
        text.getStyleClass().addAll("quick-link-text", "text-secondary");
        text.setWrapText(true);

        VBox content = new VBox(title, text);
        content.getStyleClass().add("quick-link-content");

        setGraphic(content);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        accessibleTextProperty().bind(title.textProperty());

        getStyleClass().add("quick-link");
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);
        setOnAction(event -> onOpen.run());
    }

    @Override
    protected double computePrefHeight(double width) {
        return super.computePrefHeight(width < 0 ? prefWidth(-1) : width);
    }
}
