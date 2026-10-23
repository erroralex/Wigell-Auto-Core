package com.wac.autocore.logging;

/**
 * Nycklar för MDC (Mapped Diagnostic Context) som används för att spåra
 * en användaroperation genom loggen.
 *
 * <p><b>Viktigt:</b> värdena måste stämma exakt med {@code %X{...}} i
 * {@code LOG_PATTERN} i {@code autocore-app/src/main/resources/logback-spring.xml}.
 * Byter du namn på en nyckel här måste mönstret ändras där också,
 * annars blir fältet tomt i loggen utan att något kompileringsfel uppstår.
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
        // Ska inte instansieras
    }
}
