package com.wac.autocore.exception;

import com.wac.autocore.model.BookingStatus;

/**
 * <b>BookingLockedException</b>
 * <p>Responsibility: Thrown when someone tries to add or remove services on a booking
 * that is no longer editable.</p>
 * <p><b>Example usage:</b></p>
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

