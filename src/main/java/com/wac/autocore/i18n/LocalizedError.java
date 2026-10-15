package com.wac.autocore.i18n;

import com.wac.autocore.exception.handler.ErrorReport;
import java.util.Objects;

/**
 * <b>LocalizedError</b>
 * <p>Ansvar: Resultatet av en översatt {@link ErrorReport}, färdig att visa i en dialog eller skriva ut.</p>
 */
public final class LocalizedError {

    private final String title;
    private final String message;

    public LocalizedError(String title, String message) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalizedError)) {
            return false;
        }
        LocalizedError other = (LocalizedError) obj;
        return Objects.equals(title, other.title)
                && Objects.equals(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, message);
    }

    @Override
    public String toString() {
        return "LocalizedError{" +
                "title='" + title + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
