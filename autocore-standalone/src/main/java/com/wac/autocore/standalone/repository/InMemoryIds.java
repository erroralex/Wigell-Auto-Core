package com.wac.autocore.standalone.repository;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

final class InMemoryIds {
    private final AtomicInteger next = new AtomicInteger(1);

    int next() {
        return next.getAndIncrement();
    }

    static int idOf(Object entity) {
        try {
            Field field = findIdField(entity.getClass());
            field.setAccessible(true);
            return field.getInt(entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read id", e);
        }
    }

    static void setId(Object entity, int id) {
        try {
            Field field = findIdField(entity.getClass());
            field.setAccessible(true);
            field.setInt(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot set id", e);
        }
    }

    private static Field findIdField(Class<?> type) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField("id");
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("id");
    }
}