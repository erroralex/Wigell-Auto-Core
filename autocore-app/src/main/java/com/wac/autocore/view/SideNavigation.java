package com.wac.autocore.view;

import com.wac.autocore.view.component.BrandMark;
import com.wac.autocore.view.component.Gear;
import com.wac.autocore.view.component.LanguageSwitch;
import com.wac.autocore.view.i18n.LanguageManager;
import com.wac.autocore.view.util.ViewPermissionUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>SideNavigation</b>
 * <p>Responsibility: Builds the side menu, one section per {@link NavigationGroup}, marks the active
 * choice, switches view in {@link MainLayout} on click and holds the language switch.</p>
 */
public class SideNavigation extends VBox {

    private final Map<NavigationItem, Button> buttons = new EnumMap<>(NavigationItem.class);

    private final MainLayout mainLayout;
    private final LanguageManager lang = LanguageManager.getInstance();
    private Button activeButton;

    public SideNavigation(MainLayout mainLayout, Runnable onLogout) {
        this.mainLayout = mainLayout;

        this.getStyleClass().add("side-navigation");

        // Icon and app name at the top of the menu, with the signed-in role right under it
        this.getChildren().addAll(new BrandMark(lang.get("app.brand")), createRoleLabel());

        // One section per group, in the groups' declaration order
        Map<NavigationGroup, List<NavigationItem>> itemsByGroup = Arrays.stream(NavigationItem.values())
                .collect(Collectors.groupingBy(NavigationItem::getGroup,
                        () -> new EnumMap<>(NavigationGroup.class),
                        Collectors.toList()));
        itemsByGroup.forEach((group, items) -> this.getChildren().add(createGroup(group, items)));

        // Pushes the gears, language switch and log out button down to the bottom of the menu
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        this.getChildren().addAll(spacer, createDecoration(), new LanguageSwitch(), createLogoutButton(onLogout));

        // Show the first section as selected at startup
        select(NavigationItem.values()[0]);
    }

    // A section: an optional label followed by the group's buttons.
    private VBox createGroup(NavigationGroup group, List<NavigationItem> items) {
        VBox section = new VBox();
        section.getStyleClass().add("nav-group");

        if (group.hasLabel()) {
            section.getChildren().add(createSectionLabel(group));
        }

        Button[] groupButtons = new Button[items.size()];
        for (int i = 0; i < items.size(); i++) {
            NavigationItem item = items.get(i);
            Button btn = createNavButton(item);
            buttons.put(item, btn);
            groupButtons[i] = btn;
        }
        section.getChildren().addAll(groupButtons);

        BooleanBinding anyVisible = Bindings.createBooleanBinding(
                () -> Arrays.stream(groupButtons).anyMatch(Button::isVisible),
                Arrays.stream(groupButtons).map(Button::visibleProperty).toArray(ObservableValue[]::new));
        section.visibleProperty().bind(anyVisible);
        section.managedProperty().bind(anyVisible);

        return section;
    }

    // Section label followed by a thin rule that fills the rest of the row, so it reads as a heading
    // rather than an item. Upper case is set here.
    private HBox createSectionLabel(NavigationGroup group) {
        StringBinding text = lang.bind(group.getKey());
        Label label = new Label();
        label.textProperty().bind(Bindings.createStringBinding(
                () -> text.get().toUpperCase(lang.getLocale()), text));
        label.getStyleClass().add("nav-section-text");

        Region rule = new Region();
        rule.getStyleClass().add("nav-section-rule");
        HBox.setHgrow(rule, Priority.ALWAYS);

        HBox header = new HBox(label, rule);
        header.getStyleClass().add("nav-section");
        return header;
    }

    // The signed-in user's role under the app name. Bound, so it follows a language switch.
    private Label createRoleLabel() {
        Label roleLabel = new Label();
        roleLabel.getStyleClass().add("nav-user");
        ViewPermissionUtil.currentRole().ifPresent(role ->
                roleLabel.textProperty().bind(lang.bind("role." + role.name())));
        return roleLabel;
    }

    // Last item in the menu, under the language switch
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

    // A pair of meshing outline gears above the language switch
    private HBox createDecoration() {
        HBox gears = new HBox(new Gear("gear-menu"));
        gears.getStyleClass().add("nav-decoration");
        return gears;
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

    public void select(NavigationItem item) {
        select(item, buttons.get(item));
    }
}
