package com.wac.autocore.view.component;

import javafx.scene.layout.Region;

/**
 * <b>Gear</b>
 * <p>Responsibility: A decorative outline gear. The shape, size and color come from CSS
 * (.gear plus a size class), so it scales with the zoom. Never takes clicks or focus.</p>
 */
public class Gear extends Region {

    public Gear(String sizeClass) {
        getStyleClass().addAll("gear", sizeClass);
        // Decoration only: clicks pass through to whatever is behind it
        setMouseTransparent(true);
    }
}
