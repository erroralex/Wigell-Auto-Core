package com.wac.autocore.view;

import com.wac.autocore.view.component.BrandMark;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.ViewPermissionUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

    public SideNavigation(MainLayout mainLayout, Runnable onLogout) {
        this.mainLayout = mainLayout;

        this.getStyleClass().add("side-navigation");

        // Icon and app name at the top of the menu, with the signed-in role right under it
        this.getChildren().addAll(new BrandMark(lang.get("app.brand")), createRoleLabel());

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

        // Pushes the language and log out buttons down to the bottom of the menu
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        this.getChildren().addAll(spacer, createLanguageButton(), createLogoutButton(onLogout));

        // Show the first section as selected at startup
        if (firstButton != null) {
            select(NavigationItem.values()[0], firstButton);
        }
    }

    // The signed-in user's role under the app name. Bound, so it follows a language switch.
    private Label createRoleLabel() {
        Label roleLabel = new Label();
        roleLabel.getStyleClass().add("nav-user");
        ViewPermissionUtil.currentRole().ifPresent(role ->
                roleLabel.textProperty().bind(lang.bind("role." + role.name())));
        return roleLabel;
    }

    // Last item in the menu, under the language toggle
    private Button createLogoutButton(Runnable onLogout) {
        Button btn = new Button();
        btn.textProperty().bind(lang.bind("nav.logout"));
        btn.getStyleClass().add("nav-logout");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> onLogout.run());
        return btn;
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

    // Signed-in role and a Log out button
    private VBox createAccountBox(Runnable onLogout) {
        Label roleLabel = new Label();
        roleLabel.getStyleClass().add("nav-user");
        ViewPermissionUtil.currentRole().ifPresent(role ->
                roleLabel.textProperty().bind(lang.bind("role." + role.name())));

        Button logoutButton = new Button();
        logoutButton.textProperty().bind(lang.bind("nav.logout"));
        logoutButton.getStyleClass().add("nav-logout");
        logoutButton.setOnAction(e -> onLogout.run());

        VBox box = new VBox(roleLabel, logoutButton);
        box.getStyleClass().add("nav-account");
        return box;
    }
}
