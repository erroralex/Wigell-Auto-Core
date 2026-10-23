package com.wac.autocore.service;

import com.wac.autocore.model.Booking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Collects the booking and any price changes in a result object from BookingService.createFromPrevious().
public final class BookingCopyResult {

    private final Booking booking;
    private final List<BookingPriceChange> priceChanges;

    // Prevents the caller from changing the list of price changes by creating an immutable copy of the list.
    public BookingCopyResult(Booking booking,
                             List<BookingPriceChange> priceChanges) {
        this.booking = booking;
        this.priceChanges = Collections.unmodifiableList(
                new ArrayList<>(priceChanges)
        );
    }

    public Booking getBooking() {
        return booking;
    }

    public List<BookingPriceChange> getPriceChanges() {
        return priceChanges;
    }

}
