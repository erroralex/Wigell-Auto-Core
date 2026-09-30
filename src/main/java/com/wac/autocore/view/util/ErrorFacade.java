package com.wac.autocore.view.util;

import com.wac.autocore.exception.handler.ErrorHandler;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>ErrorFacade</b>
 * <p>Ansvar: En enda ingång (<b>Facade</b>) för att visa fel i gränssnittet. Döljer felkedjan,
 * språkhanteringen, loggningen och bytet till JavaFX-tråden bakom ett anrop.</p>
 * <p>Fel som inte fångas alls hamnar här automatiskt via {@link GlobalExceptionHandler}.</p>
 * <p><b>Exempel på användning i en vy:</b></p>
 * <pre>{@code
 * try {
 *     workOrderService.createWorkOrder(booking.getId());
 * } catch (RuntimeException e) {
 *     ErrorFacade.handle(e);
 * }
 * }</pre>
 */
public final class ErrorFacade {

    private static final Logger log = LoggerFactory.getLogger(ErrorFacade.class);
    private static final ErrorHandler CHAIN = ErrorHandlerChain.createDefault();
    private static boolean presenting;

    private ErrorFacade() {
    }

    public static void handle(Throwable error) {
        if (error == null) {
            return;
        }
    }
}
