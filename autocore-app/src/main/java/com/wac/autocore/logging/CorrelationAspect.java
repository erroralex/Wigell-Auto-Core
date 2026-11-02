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

/**
 * <b>CorrelationAspect</b>
 * <p>Responsibility: Gives each user operation a correlation id in the MDC, so all log lines
 * from the same operation can be tied together. Wraps all public methods in the service layer.</p>
 * <p>Only the outermost call creates and clears the id. Inner service calls keep it.</p>
 * <p>Runs first of all aspects, so that logging from transactions also gets the id.</p>
 * <p>When an operation fails, the id is attached to the exception via {@link CorrelationIds},
 * so the UI can show it as a reference after the MDC has been cleared.</p>
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
        // Inner call: the id already exists, do nothing.
        if (MDC.get(LogKeys.CORR_ID) != null) {
            return pjp.proceed();
        }

        MDC.put(LogKeys.CORR_ID, CorrelationIds.newId());
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
            CorrelationIds.tag(t, MDC.get(LogKeys.CORR_ID));
            log.debug("Failed after {} ms: {}", elapsedMs(start), SafeLog.describe(t));
            throw t;
        } finally {
            MDC.remove(LogKeys.CORR_ID);
            MDC.remove(LogKeys.OP);
            MDC.remove(LogKeys.USER);
            MDC.remove(LogKeys.WORK_ORDER_ID);
        }
    }

    // TODO: replace with the logged-in user when authentication is done.
    private String currentUser() {
        return System.getProperty("user.name", "unknown");
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
