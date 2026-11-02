package com.wac.autocore.view;

/**
 * <b>NavigationGroup</b>
 * <p>Responsibility: Defines the sections of the side menu, in display order, and the language key
 * of each section's label. The side menu is built per group, so a {@link NavigationItem} only has to
 * name its group to end up in the right place.</p>
 */
public enum NavigationGroup {
    MAIN(null),
    REGISTER("nav.section.register"),
    OPERATIONS("nav.section.operations"),
    FINANCE("nav.section.finance");

    private final String key;

    NavigationGroup(String key) {
        this.key = key;
    }

    public boolean hasLabel() {
        return key != null;
    }

    public String getKey() {
        return key;
    }
}
