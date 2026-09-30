package com.wac.autocore.exception;

/**
 * <b>BookingLockedException</b>
 * <p>Ansvar: Kastas när någon försöker lägga till eller ta bort tjänster på en bokning där
 * verkstadsarbetet redan har påbörjats</p>
 * <p><b>Exempel på användning:</b></p>
 * <pre>{@code
 * if (!"BOOKED".equals(booking.getStatus())) {
 *     throw new BookingLockedException(booking.getId(), booking.getStatus());
 * }
 * }</pre>
 */
public class BookingLockedException extends DomainException {

    private final int bookingId;
    private final String status;

    public BookingLockedException(int bookingId, String status) {
        super("Booking " + bookingId + "is locked (status " + status + ")",
                "error.booking", "error.bookingLocked", String.valueOf(bookingId));
        this.bookingId = bookingId;
        this.status = status;
    }

    public int getBookingId() {
        return bookingId;
    }

    public String getStatus() {
        return status;
    }
}
