package com.wac.autocore.view;

import com.wac.autocore.view.component.QuickLink;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.function.Consumer;

/**
 * <b>HomeView</b>
 * <p>Responsibility: Start view that shows the logo, a welcome heading and a short guide,
 * centered in one column.</p>
 */
public class HomeView extends BaseView {

    private static final Logger log = LoggerFactory.getLogger(HomeView.class);

    private final Consumer<NavigationItem> onNavigate;

    private static final String LOGO_PATH = "assets/wigell-auto-logo.png";

    private static final double LOGO_TO_FONT_RATIO = 28;

    private final Label headingLabel = new Label();
    private final Label bodyLabel = new Label();

    public HomeView(Consumer<NavigationItem> onNavigate) {
        this.onNavigate = onNavigate;

        headingLabel.textProperty().bind(lang.bind("home.heading"));
        headingLabel.getStyleClass().add("home-heading");
        headingLabel.setWrapText(true);
        headingLabel.setMaxWidth(Double.MAX_VALUE);

        bodyLabel.textProperty().bind(lang.bind("home.body"));
        bodyLabel.getStyleClass().addAll("home-body", "text-secondary");
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(Double.MAX_VALUE);

        getStyleClass().add("home-view");
        initView();
    }

    // Shows the logo, or the app's name as text if the image cannot be loaded
    private Label createLogo() {
        Label logo = new Label();
        URL logoUrl = getClass().getResource(LOGO_PATH);

        if (logoUrl != null) {
            ImageView imageView = new ImageView(new Image(logoUrl.toExternalForm()));
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.fitWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> bodyLabel.getFont().getSize() * LOGO_TO_FONT_RATIO,
                    bodyLabel.fontProperty()));
            imageView.getStyleClass().add("home-logo"); // CSS class for the shadow behind the logo
            logo.setGraphic(imageView);
            logo.setAlignment(Pos.TOP_CENTER);
        } else {
            log.warn("Could not load logo {}, using text placeholder", LOGO_PATH);
            logo.textProperty().bind(lang.bind("app.title"));
        }
        return logo;
    }

    @Override
    protected String getTitleKey() {
        return null;
    }

    @Override
    protected void buildHeader() { // Overrides BaseView: no page title on the home view.
    }

    // One centered column with a max width: logo, heading, body text and shortcuts
    @Override
    protected void buildContent() {
        VBox column = new VBox(createLogo(), headingLabel, bodyLabel, createQuickLinks());
        column.getStyleClass().add("home-content");
        getChildren().add(column);
    }

    // Shortcuts to the most used views. Every role may read all three
    private HBox createQuickLinks() {
        HBox links = new HBox(
                createQuickLink(NavigationItem.BOOKINGS, "home.link.bookings"),
                createQuickLink(NavigationItem.CUSTOMERS, "home.link.customers"),
                createQuickLink(NavigationItem.WORK_ORDERS, "home.link.workOrders"));
        links.getStyleClass().add("quick-links");
        links.setMaxHeight(Region.USE_PREF_SIZE);
        return links;
    }

    // The card's title reuses the menu's text, so the shortcut and the menu always match
    private QuickLink createQuickLink(NavigationItem item, String textKey) {
        QuickLink link = new QuickLink(item.getKey(), textKey, () -> onNavigate.accept(item));
        HBox.setHgrow(link, Priority.ALWAYS);
        return link;
    }
}
