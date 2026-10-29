package com.wac.autocore;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wac.autocore.model.WorkOrderEvent;
import com.wac.autocore.model.WorkOrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ch.qos.logback.classic.Level;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class WorkOrderEventListenerTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Logger listenerLogger;
    private ListAppender<ILoggingEvent> appender;
    private Level previousLevel;

    @BeforeEach
    void setUp() {
        listenerLogger = (Logger) LoggerFactory.getLogger("com.wac.autocore.logging.WorkOrderEventLogger");
        previousLevel = listenerLogger.getLevel();
        listenerLogger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.setContext(listenerLogger.getLoggerContext());
        appender.start();
        listenerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        listenerLogger.detachAppender(appender);
        listenerLogger.setLevel(previousLevel);
        listenerLogger.detachAppender(appender);
        appender.stop();
        appender.stop();
    }

    @Test
    void publishesOneInfoLogAfterCommit() {
        WorkOrderEvent event = new WorkOrderEvent(
                42,
                WorkOrderType.PLANNED,
                WorkOrderEvent.WorkOrderEventAction.CREATED,
                LocalDateTime.now()
        );

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                publisher.publishEvent(event)
        );

        assertThat(appender.list)
                .hasSize(1)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(msg ->
                        assertThat(msg).contains("action=CREATED")
                                .contains("workOrderId=42")
                                .contains("type=PLANNED")
                                .contains("user=")
                );
    }

    @Test
    void doesNotLogWhenTransactionsRollsBack() {
        WorkOrderEvent event = new WorkOrderEvent(
                99,
                WorkOrderType.PLANNED,
                WorkOrderEvent.WorkOrderEventAction.COMPLETED,
                LocalDateTime.now()
        );

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publisher.publishEvent(event);
            status.setRollbackOnly();
        });

        assertThat(appender.list).isEmpty();
    }
}