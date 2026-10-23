package com.wac.autocore.service;

import com.wac.autocore.logging.CorrelationAspect;
import com.wac.autocore.logging.LogKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>CorrelationAspectTest</b>
 * <p>Responsibility: Proves that an operation gets one and the same correlation id, even through inner
 * service calls, that the next operation gets a new id and that the MDC is cleaned up afterwards.</p>
 */
class CorrelationAspectTest {

    private final List<String> seen = new ArrayList<>();

    public static class InnerFake {
        private final List<String> seen;

        public InnerFake(List<String> seen) {
            this.seen = seen;
        }

        public void work() {
            seen.add(MDC.get(LogKeys.CORR_ID));
        }
    }

    public static class OuterFake {
        private final InnerFake inner;
        private final List<String> seen;

        public OuterFake(InnerFake inner, List<String> seen) {
            this.inner = inner;
            this.seen = seen;
        }

        public void run() {
            seen.add(MDC.get(LogKeys.CORR_ID));
            inner.work();
        }

        public void fail() {
            throw new IllegalStateException("test");
        }
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Inner service call keeps the same corrId")
    void nestedCallKeepsSameId() {
        outer().run();

        assertEquals(2, seen.size());
        assertNotNull(seen.get(0));
        assertEquals(seen.get(0), seen.get(1));
    }

    @Test
    @DisplayName("Two operations in a row get different corrIds")
    void eachOperationGetsNewId() {
        OuterFake outer = outer();
        outer.run();
        outer.run();

        assertNotEquals(seen.get(0), seen.get(2));
    }

    @Test
    @DisplayName("MDC is empty after the operation")
    void mdcClearedAfterCall() {
        outer().run();

        assertNull(MDC.get(LogKeys.CORR_ID));
        assertNull(MDC.get(LogKeys.OP));
        assertNull(MDC.get(LogKeys.USER));
    }

    @Test
    @DisplayName("MDC is empty after an exception")
    void mdcClearedAfterException() {
        assertThrows(IllegalStateException.class, () -> outer().fail());

        assertNull(MDC.get(LogKeys.CORR_ID));
    }

    private OuterFake outer() {
        InnerFake inner = proxy(new InnerFake(seen));
        return proxy(new OuterFake(inner, seen));
    }

    private <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new CorrelationAspect());
        return factory.getProxy();
    }
}
