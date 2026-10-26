package com.wac.autocore.logging;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * <b>CorrelationIds</b>
 * <p>Responsibility: Remembers which correlation id an exception was thrown under, so the id
 * survives after {@link CorrelationAspect} has cleared the MDC.</p>
 * <p>Keys are held weakly: once nothing references the exception, the entry is removed by the GC.
 * {@link Throwable} does not override {@code equals}, so lookups are by identity.</p>
 *
 * @see LogKeys#CORR_ID
 */
public class CorrelationIds {

    private static final int MAX_CAUSE_DEPTH = 20;

    private static final Map<Throwable, String> IDS = Collections.synchronizedMap(new WeakHashMap<>());

    private CorrelationIds() {
        // Not meant to be instantiated
    }

    // Links an exception to a correlation id. The first id wins, so an outer call cannot overwrite an inner one.
    public static void tag(Throwable error, String corrId) {
        if (error == null || corrId == null) {
            return;
        }
        IDS.putIfAbsent(error, corrId);
    }

    // Returns the first id found in the cause chain, or null if none of the exceptions are tagged.
    public static String find(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            String id = IDS.get(current);
            if (id != null) {
                return id;
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }

    // Creates a new short correlation id. The only place where the id format is defined.
    public static String newId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
