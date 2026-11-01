package com.wac.autocore.standalone.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Set;

/**
 * <b>ValidatingProxy</b>
 * <p>Responsibility: Checks the Bean Validation constraints declared on an interface
 * before the call reaches the real object, the job Spring's method validation does in autocore-app.</p>
 * <p>A <b>Proxy</b> built with the JDK's dynamic proxy, so one class covers every
 * method of the interface. A violation throws {@link ConstraintViolationException},
 * which {@code ValidationErrorHandler} turns into the same error text as in the app.</p>
 */
public final class ValidatingProxy implements InvocationHandler {

    private static final ExecutableValidator VALIDATOR = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator()
            .forExecutables();

    private final Object target;

    private ValidatingProxy(Object target) {
        this.target = target;
    }

    // Wraps the target in a proxy that implements the given interface.
    public static <T> T of(Class<T> type, T target) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(target, "target");
        if (!type.isInterface()) {
            throw new IllegalArgumentException(type.getName() + " is not an interface");
        }
        Object proxy = Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[]{type}, new ValidatingProxy(target));
        return type.cast(proxy);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object[] arguments = args == null ? new Object[0] : args;

        // toString, equals and hashCode have no constraints and go straight through
        if (method.getDeclaringClass() != Object.class) {
            Set<ConstraintViolation<Object>> violations =
                    VALIDATOR.validateParameters(target, method, arguments);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException e) {
            // Rethrow the service's own exception
            throw e.getCause();
        }
    }
}
