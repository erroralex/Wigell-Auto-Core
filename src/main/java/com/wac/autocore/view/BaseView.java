package com.wac.autocore.view;

import com.wac.autocore.service.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public abstract class BaseView extends VBox {

    protected final LanguageManager lang = LanguageManager.getInstance();
    protected abstract String getTitleKey();
    protected Label title;

    public final void initView() {
        buildHeader();
        buildContent();
        buildFooter();
        applyStyling();
    }

    protected void buildHeader() {
        title = new Label(lang.get(getTitleKey()));
        title.getStyleClass().add("text-title");

        getChildren().add(title);
    }

    protected abstract void buildContent();

    protected void buildFooter() {

    }

    private void applyStyling() {
        getStyleClass().add("content-area");
        setSpacing(20);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_LEFT);
    }



}
