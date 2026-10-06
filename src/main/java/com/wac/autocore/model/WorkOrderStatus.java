package com.wac.autocore.model;

import java.util.*;

public enum WorkOrderStatus {

    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED;

    static {
        Map<WorkOrderStatus, Set<WorkOrderStatus>> map = new EnumMap<>(WorkOrderStatus.class);

        map.put(DRAFT, EnumSet.of(CONFIRMED, CANCELLED));
        map.put(CONFIRMED, EnumSet.of(IN_PROGRESS, CANCELLED, DRAFT));
        map.put(IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED));
        map.put(CANCELLED, EnumSet.of(DRAFT));
        map.put(COMPLETED, EnumSet.noneOf(WorkOrderStatus.class));

        ALLOWED = Collections.unmodifiableMap(map);
    }

    public boolean canChangeTo(WorkOrderStatus target) {
        return ALLOWED.getOrDefault(this, Collections.emptySet()).contains(target);
    }
}
