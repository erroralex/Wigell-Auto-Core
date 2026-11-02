package com.wac.autocore.view.component;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * <b>BrandMark</b>
 * <p>Responsibility: The Wigell Auto icon on a light tile next to the app name. Used at the top of the
 * side menu and on the login screen; each place styles it through its own parent CSS class.</p>
 */
public class BrandMark extends HBox {

    private static final Logger log = LoggerFactory.getLogger(BrandMark.class);

    private static final String ICON_PATH = "/com/wac/autocore/view/assets/wigell_auto_icon.png";

    // The icon is sized from the name's font, so it follows the Ctrl+scroll zoom like the text does
    private static final double ICON_TO_FONT_RATIO = 2.2;

    public BrandMark(String name) {
        getStyleClass().add("brand-mark");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("brand-name");

        getChildren().addAll(createTile(nameLabel), nameLabel);
    }

    // Light tile behind the icon, so its dark outlines stay visible on the dark side menu
    private StackPane createTile(Label nameLabel) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("brand-tile");

        URL iconUrl = BrandMark.class.getResource(ICON_PATH);
        if (iconUrl == null) {
            log.warn("Could not load icon {}", ICON_PATH);
            return tile;
        }

        ImageView icon = new ImageView(new Image(iconUrl.toExternalForm()));
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        icon.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> nameLabel.getFont().getSize() * ICON_TO_FONT_RATIO,
                nameLabel.fontProperty()));

        tile.getChildren().add(icon);
        return tile;
    }
}
