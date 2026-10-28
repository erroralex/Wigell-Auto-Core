package com.wac.autocore.logging;

import com.wac.autocore.model.WorkOrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WorkOrderEventLogger {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderEventLogger.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkOrderEvent(WorkOrderEvent event) {
        String user = currentUser();
        log.info("action={} workOrderId={} type={} user={}",
                event.action(),
                event.workOrderId(),
                event.workOrderType(),
                user);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "system";
        }
        return auth.getName();
    }
}
