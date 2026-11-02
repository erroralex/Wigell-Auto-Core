package com.wac.autocore.view.component;

import javafx.scene.layout.Region;

/**
 * <b>Gear</b>
 * <p>Responsibility: A decorative pair of meshing outline gears, a 12-tooth and an 8-tooth gear with
 * the same tooth size. Both are one CSS shape, so they stay meshed at any size and zoom.
 * The size comes from a size class (.gear plus a size class). Never takes clicks or focus.</p>
 */
public class Gear extends Region {

    public Gear(String sizeClass) {
        getStyleClass().addAll("gear", sizeClass);
        // Decoration only: clicks pass through to whatever is behind it
        setMouseTransparent(true);
    }
}
