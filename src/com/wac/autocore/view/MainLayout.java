package com.wac.autocore.view;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * <b>MainLayout</b>
 * <p>Ansvar: Applikationens huvudskal. Håller sidonavigeringen och växlar dynamiskt vilken vy som visas i mitten.</p>
 */
public class MainLayout extends BorderPane {

    private final SideNavigation sideNavigation;

    public MainLayout(){
        this.getStyleClass().add("main-layout");

        this.sideNavigation = new SideNavigation(this);
        this.setLeft(sideNavigation);
        sideNavigation.setPrefWidth(220);

        // Kundvyn visas som standard vid uppstart
        setContent(new CustomerView());
    }

    // Byter ut det centrala innehållet. Anropas av SideNavigation vid varje sektionsbyte.
    public void setContent(Node view) {
        this.setCenter(view);
    }
}
