package com.wac.autocore.exception.handler;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

// First link in the error chain so a security error is never masked by a later link.
// Separates a user who is not logged in (AuthenticationCredentialsNotFoundException)
// from a user who is logged in, but lacks permission (AccessDeniedException).
public class SecurityErrorHandler extends ErrorHandler {

    // Answers yes if either security exception is anywhere in the cause chain
    @Override
    protected boolean canHandle(Throwable error) {
        return findCause(error, AuthenticationCredentialsNotFoundException.class) != null
                || findCause(error, AccessDeniedException.class) != null;
    }

    @Override
    protected ErrorReport createReport(Throwable error) {
        AuthenticationCredentialsNotFoundException notAuthenticated =
                findCause(error, AuthenticationCredentialsNotFoundException.class);
        if (notAuthenticated != null) {
            return report("error.notAuthenticated", notAuthenticated);
        }
        return report("error.accessDenied", findCause(error, AccessDeniedException.class));
    }

    private ErrorReport report(String messageKey, Throwable cause) {
        return new ErrorReport(ErrorReport.Severity.EXPECTED, "error.title", messageKey, null, cause);
    }
}
