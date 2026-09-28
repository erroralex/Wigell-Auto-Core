package com.wac.autocore.view;

import com.wac.autocore.service.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * <b>SideNavigation</b>
 * <p>Ansvar: Bygger sidomenyns navigeringsknappar, markerar aktivt val, byter vy i {@link MainLayout} vid klick och växlar språk.</p>
 */
public class SideNavigation extends VBox {

    private final MainLayout mainLayout;
    private final LanguageManager lang = LanguageManager.getInstance();
    private Button activeButton;

    public SideNavigation(MainLayout mainLayout) {
        this.mainLayout = mainLayout;

        this.getStyleClass().add("side-navigation");
        this.setSpacing(8);
        this.setPadding(new Insets(20));

        // Bygg en navigeringsknapp för varje sektion definierad i NavigationItem,
        // så nya sektioner läggs till bara genom att utöka enumen
        Button firstButton = null;
        for (NavigationItem item : NavigationItem.values()) {
            Button btn = createNavButton(item);
            this.getChildren().add(btn);
            if (firstButton == null) {
                firstButton = btn;
            }
        }

        // Trycker ner språkknappen till menyns botten
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        this.getChildren().addAll(spacer, createLanguageButton());

        // Visa första sektionen som vald vid uppstart
        if (firstButton != null) {
            select(NavigationItem.values()[0], firstButton);
        }
    }

    private Button createNavButton(NavigationItem item) {
        Button btn = new Button();
        // Bind istället för setText: texten följer med när språket växlas
        btn.textProperty().bind(lang.bind(item.getKey()));
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        btn.setOnAction(e -> select(item, btn));
        return btn;
    }

    private Button createLanguageButton() {
        Button btn = new Button();
        btn.textProperty().bind(lang.bind("nav.toggleLanguage"));
        btn.getStyleClass().addAll("nav-button", "nav-button-language");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        btn.setOnAction(e -> lang.toggleLanguage());
        return btn;
    }

    // Uppdaterar CSS-klass för att visuellt markera vilken sektion som är vald
    private void setActiveButton(Button newActiveButton) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        newActiveButton.getStyleClass().add("nav-button-active");
        activeButton = newActiveButton;
    }

    private void select(NavigationItem item, Button btn) {
        mainLayout.show(item);
        setActiveButton(btn);
    }
}
