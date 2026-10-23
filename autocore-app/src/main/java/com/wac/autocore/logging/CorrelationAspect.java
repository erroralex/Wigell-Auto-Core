package com.wac.autocore.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * <b>CorrelationAspect</b>
 * <p>Ansvar: Ger varje användaroperation ett korrelations-id i MDC, så att alla loggrader
 * från samma operation kan kopplas ihop. Ligger runt alla publika metoder i service-lagret.</p>
 * <p>Bara det yttersta anropet skapar och städar id:t. Inre service-anrop behåller det.</p>
 * <p>Körs först av alla aspekter, så att även loggning från transaktioner får id:t.</p>
 *
 * @see LogKeys
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationAspect {

    private static final Logger log = LoggerFactory.getLogger(CorrelationAspect.class);

    @Around("execution(public * com.wac.autocore.service..*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // Inre anrop: id:t finns redan, gör ingenting.
        if (MDC.get(LogKeys.CORR_ID) != null) {
            return pjp.proceed();
        }

        MDC.put(LogKeys.CORR_ID, UUID.randomUUID().toString().substring(0, 8));
        MDC.put(LogKeys.OP, pjp.getSignature().getDeclaringType().getSimpleName()
                + "." + pjp.getSignature().getName());
        MDC.put(LogKeys.USER, currentUser());

        long start = System.nanoTime();
        log.debug("Start");
        try {
            Object result = pjp.proceed();
            log.debug("Done in {} ms", elapsedMs(start));
            return result;
        } catch (Throwable t) {
            log.debug("Failed after {} ms: {}", elapsedMs(start), t.toString());
            throw t;
        } finally {
            MDC.remove(LogKeys.CORR_ID);
            MDC.remove(LogKeys.OP);
            MDC.remove(LogKeys.USER);
            MDC.remove(LogKeys.WORK_ORDER_ID);
        }
    }

    // TODO: byt mot inloggad användare när autentiseringen är klar.
    private String currentUser() {
        return System.getProperty("user.name", "unknown");
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
