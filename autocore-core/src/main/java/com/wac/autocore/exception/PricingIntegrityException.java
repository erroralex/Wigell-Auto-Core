package com.wac.autocore.exception;

/**
 * <b>PricingIntegrityException</b>
 * <p>Responsibility: Thrown when a price snapshot is missing or invalid, e.g. if a work order would
 * be created from a booking line without a frozen price. Protects the requirement that historical prices are never
 * recalculated from the catalog.</p>
 * <p><b>Example usage:</b></p>
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
