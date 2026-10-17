package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

// Gemensam regel för bokningens och arbetsorderns tjänsterader:
// en vald tjänst som redan har en rad behåller den (avtalat pris och tid), en ny tjänst snapshotas
// från katalogen och en tjänst som inte längre väljs tas bort. Samma tjänst vald två gånger ger en rad.
// Returnerar en ny lista och rör inte de befintliga raderna, så att anroparen kan validera klart innan den skriver.
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
