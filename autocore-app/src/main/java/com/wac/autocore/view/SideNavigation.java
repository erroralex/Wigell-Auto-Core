package com.wac.autocore.view;

import com.wac.autocore.view.i18n.LanguageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * <b>SideNavigation</b>
 * <p>Responsibility: Builds the side menu's navigation buttons, marks the active choice, switches view in {@link MainLayout} on click and toggles language.</p>
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

        // Build a navigation button for each section defined in NavigationItem,
        // so new sections are added just by extending the enum
        Button firstButton = null;
        for (NavigationItem item : NavigationItem.values()) {
            Button btn = createNavButton(item);
            this.getChildren().add(btn);
            if (firstButton == null) {
                firstButton = btn;
            }
        }

        // Pushes the language button down to the bottom of the menu
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        this.getChildren().addAll(spacer, createLanguageButton());

        // Show the first section as selected at startup
        if (firstButton != null) {
            select(NavigationItem.values()[0], firstButton);
        }
    }

    private Button createNavButton(NavigationItem item) {
        Button btn = new Button();
        // Bind instead of setText: the text follows along when the language is switched
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

    // Updates the CSS class to visually mark which section is selected
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
