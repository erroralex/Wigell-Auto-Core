package com.wac.autocore.logging;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    @EventListener
    public void onSuccessfulLogin(AuthenticationSuccessEvent successEvent) {
        String username = successEvent.getAuthentication().getName();

        log.info("event=login_success username={}", username);
    }

    @EventListener
    public void onFailedLogin(AbstractAuthenticationFailureEvent failureEvent) {
        String username = failureEvent.getAuthentication().getName();

        log.warn("event=login_failure username={}", username);
    }

    @EventListener
    public void onDeniedAccess(AuthorizationDeniedEvent<?> authorizationDeniedEvent) {
        Authentication authentication = authorizationDeniedEvent.getAuthentication().get();
        String username = authentication == null || authentication.getName() == null
                || authentication.getName().isEmpty()
                ? "UNIDENTIFIED/ANONYMOUS"
                : authentication.getName();

        Object securedObject = authorizationDeniedEvent.getObject();
        String operation = "unknown";

        if (securedObject instanceof MethodInvocation invocation) {
            operation = invocation.getMethod().getDeclaringClass().getSimpleName()
                    + "." + invocation.getMethod().getName();
        }

        log.warn("event=access_denied username={} operation={}", username, operation);
    }



}
