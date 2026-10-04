package com.wac.autocore.exception;

/**
 * <b>EntityNotFoundException</b>
 * <p>Ansvar: Kastas när en entitet som refereras med id inte finns i databasen.
 * Ersätter generiska {@code RuntimeException("... not found")} i tjänstelagret, så att vyn kan
 * skilja "hittades inte" från andra fel.</p>
 * <p><b>Exempel på användning:</b></p>
 * <pre>{@code
 * ServiceItem item = serviceItemRepository.findById(id)
 *         .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound"));
 * }</pre>
 */
public class EntityNotFoundException extends DomainException {

    private final String entityName;
    private final Object entityId;

    /* Ansvar: Använder det generiska meddelandet error.notFound */
    public EntityNotFoundException(String entityName, Object entityId) {
        this(entityName, entityId, "error.notFound");
    }

    /* Ansvar: Använder ett mer specifikt meddelande, t.ex. error.serviceNotFound */
    public EntityNotFoundException(String entityName, Object entityId, String messageKey) {
        super(entityName + " not found: " + entityId, "error.title", messageKey);
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public Object getEntityId() {
        return entityId;
    }
}