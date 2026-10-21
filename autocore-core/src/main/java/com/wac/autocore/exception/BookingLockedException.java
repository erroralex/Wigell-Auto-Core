package com.wac.autocore.exception;

import com.wac.autocore.model.BookingStatus;

/**
 * <b>BookingLockedException</b>
 * <p>Ansvar: Kastas när någon försöker lägga till eller ta bort tjänster på en bokning
 * som inte längre är redigerbar.</p>
 * <p><b>Exempel på användning:</b></p>
 * <pre>{@code
 * if (!booking.getStatus().isEditable()) {
 *     throw new BookingLockedException(booking.getId(), booking.getStatus());
 * }
 * }</pre>
 */
public class BookingLockedException extends DomainException {

    private final int bookingId;
    private final BookingStatus status;

    public BookingLockedException(int bookingId, BookingStatus status) {
        super("Booking " + bookingId + " is locked (status " + status + ")",
                "error.booking", "error.bookingLocked", String.valueOf(bookingId));
        this.bookingId = bookingId;
        this.status = status;
    }

    public int getBookingId() {
        return bookingId;
    }

    public BookingStatus getStatus() {
        return status;
    }
}

