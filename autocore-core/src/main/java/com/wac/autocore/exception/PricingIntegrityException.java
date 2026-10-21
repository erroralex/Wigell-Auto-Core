package com.wac.autocore.exception;

/**
 * <b>PricingIntegrityException</b>
 * <p>Ansvar: Kastas när en prissnapshot saknas eller är ogiltig, t.ex. om en arbetsorder skulle
 * skapas från en bokningsrad utan fryst pris. Skyddar kravet att historiska priser aldrig
 * räknas om från katalogen.</p>
 * <p><b>Exempel på användning:</b></p>
 * <pre>{@code
 * if (line.getPriceSnapshot() == null) {
 *     throw new PricingIntegrityException(
 *             "Booking " + bookingId + " line " + line.getId() + " has no price snapshot");
 * }
 * }</pre>
 */
public class PricingIntegrityException extends DomainException {

    public PricingIntegrityException(String technicalMessage) {
        super(technicalMessage, "error.title", "error.pricingIntegrity");
    }
}
