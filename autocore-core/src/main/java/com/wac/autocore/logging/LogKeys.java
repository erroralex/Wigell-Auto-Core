package com.wac.autocore.logging;

/**
 * Keys for the MDC (Mapped Diagnostic Context) used to trace
 * a user operation through the log.
 *
 * <p><b>Important:</b> the values must match {@code %X{...}} in
 * {@code LOG_PATTERN} in {@code autocore-app/src/main/resources/logback-spring.xml}
 * exactly. If you rename a key here, the pattern must be changed there as well,
 * otherwise the field is empty in the log without any compilation error.
 *
 * @see "autocore-app/src/main/resources/logback-spring.xml"
 * @see org.slf4j.MDC
 */
public final class LogKeys {

    public static final String CORR_ID = "corrId";
    public static final String OP = "op";
    public static final String USER = "user";
    public static final String WORK_ORDER_ID = "workOrderId";

    private LogKeys() {
        // Not meant to be instantiated
    }
}
