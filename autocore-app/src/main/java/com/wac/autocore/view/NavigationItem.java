package com.wac.autocore.view;

/**
 * <b>NavigationItem</b>
 * <p>Responsibility: Defines the application's navigable sections, their language keys and the
 * {@link NavigationGroup} each one belongs to. Within a group, items are shown in declaration order.</p>
 */
public enum NavigationItem {
    HOME("nav.home", NavigationGroup.MAIN),

    CUSTOMERS(       "nav.customers",       NavigationGroup.REGISTER),
    VEHICLES(        "nav.vehicles",        NavigationGroup.REGISTER),
    MECHANICS(       "nav.mechanics",       NavigationGroup.REGISTER),
    SERVICE_ITEMS(   "nav.serviceItems",    NavigationGroup.REGISTER),
    SERVICE_PACKAGES("nav.servicePackages", NavigationGroup.REGISTER),

    BOOKINGS(        "nav.bookings",        NavigationGroup.OPERATIONS),
    WORK_ORDERS(     "nav.workOrders",      NavigationGroup.OPERATIONS),

    INVOICES(        "nav.invoices",        NavigationGroup.FINANCE),
    PAYMENTS(        "nav.payments",        NavigationGroup.FINANCE);

    private final String key;
    private final NavigationGroup group;

    NavigationItem(String key, NavigationGroup group) {
        this.key = key;
        this.group = group;
    }

    public String getKey() {
        return key;
    }

    public NavigationGroup getGroup() {
        return group;
    }
}
