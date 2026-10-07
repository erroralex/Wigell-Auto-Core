package com.wac.autocore.service;

import com.wac.autocore.model.Booking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Samlar bokning och eventuella prisändringar i ett resultatobjekt från BookingService.copyBookingAsNew().
public final class BookingCopyResult {

    private final Booking booking;
    private final List<BookingPriceChange> priceChanges;

    // Hindrar anroparen från att ändra listan med prisändringar genom att skapa en oföränderlig kopia av listan.
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
