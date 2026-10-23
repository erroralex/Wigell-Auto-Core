package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

// Common rule for the booking's and the work order's service lines:
// a selected service that already has a line keeps it (agreed price and duration), a new service is snapshotted
// from the catalog and a service that is no longer selected is removed. The same service selected twice gives one line.
// Returns a new list and does not touch the existing lines, so the caller can finish validating before it writes.
final class ServiceLines {

    private ServiceLines() {
    }

    static <T> List<T> merge(List<T> current, List<ServiceItem> chosen,
                             ToIntFunction<T> serviceItemIdOf, Function<ServiceItem, T> snapshotOf) {
        List<T> next = new ArrayList<>();
        for (ServiceItem service : chosen) {
            if (service == null) {
                throw new ValidationException("error.serviceSelect");
            }
            if (find(next, service.getId(), serviceItemIdOf) != null) {
                continue;
            }
            T existing = find(current, service.getId(), serviceItemIdOf);
            next.add(existing != null ? existing : snapshotOf.apply(service));
        }
        return next;
    }

    private static <T> T find(List<T> lines, int serviceItemId, ToIntFunction<T> serviceItemIdOf) {
        for (T line : lines) {
            if (serviceItemIdOf.applyAsInt(line) == serviceItemId) {
                return line;
            }
        }
        return null;
    }
}
