package com.wac.autocore.exception;

/**
 * <b>EntityNotFoundException</b>
 * <p>Responsibility: Thrown when an entity referenced by id does not exist in the database.
 * Replaces generic {@code RuntimeException("... not found")} in the service layer, so the view can
 * tell "not found" apart from other errors.</p>
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * ServiceItem item = serviceItemRepository.findById(id)
 *         .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound"));
 * }</pre>
 */
public class EntityNotFoundException extends DomainException {

    private final String entityName;
    private final Object entityId;

    /* Responsibility: Uses the generic message error.notFound */
    public EntityNotFoundException(String entityName, Object entityId) {
        this(entityName, entityId, "error.notFound");
    }

    /* Responsibility: Uses a more specific message, e.g. error.serviceNotFound */
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