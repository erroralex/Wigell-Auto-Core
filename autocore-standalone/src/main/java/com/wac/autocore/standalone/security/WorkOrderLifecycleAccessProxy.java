package com.wac.autocore.standalone.security;

import com.wac.autocore.exception.PermissionDeniedException;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.security.AccessPolicy;
import com.wac.autocore.security.Permission;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.WorkOrderLifecycle;
import com.wac.autocore.service.WorkOrderScheduleResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <b>WorkOrderLifecycleAccessProxy</b>
 * <p>Responsibility: Checks that the current role may change work orders before the call
 * reaches the real service.</p>
 * <p>A <b>protection proxy</b> in the <b>Proxy</b> pattern (GoF). It implements the same
 * {@link WorkOrderLifecycle} interface as {@link com.wac.autocore.service.WorkOrderService} and holds a reference to it.
 * Each method asks {@link AccessPolicy} first and throws {@link PermissionDeniedException}
 * if the role lacks the permission; otherwise it delegates unchanged.</p>
 * <p>In autocore-app, Spring Security protects the services. This class gives the
 * standalone program the same rules without Spring, using the same {@link AccessPolicy}.</p>
 * <p>Not a Decorator: it adds no behaviour to the service, it only decides whether
 * the call is allowed.</p>
 */
public final class WorkOrderLifecycleAccessProxy implements WorkOrderLifecycle {

    private final WorkOrderLifecycle target;
    private final Role role;

    public WorkOrderLifecycleAccessProxy(WorkOrderLifecycle target, Role role) {
        this.target = Objects.requireNonNull(target, "target");
        this.role = Objects.requireNonNull(role, "role");
    }

    @Override
    public DropInWorkOrder createDropIn(int vehicleId, String problemDescription, List<Integer> serviceItemIds) {
        require(Permission.WORK_ORDER_WRITE);
        return target.createDropIn(vehicleId, problemDescription, serviceItemIds);
    }

    @Override
    public WorkOrderScheduleResult updateWorkOrder(int id,
                                                   List<Integer> serviceItemIds,
                                                   Integer mechanicId,
                                                   LocalDate plannedDate,
                                                   LocalTime plannedStartTime,
                                                   String customerInstructions,
                                                   String comments,
                                                   Map<Integer, Boolean> chargeFlags) {

        require(Permission.WORK_ORDER_WRITE);
        return target.updateWorkOrder(
                id,
                serviceItemIds,
                mechanicId,
                plannedDate,
                plannedStartTime,
                customerInstructions,
                comments,
                chargeFlags);
    }

    @Override
    public void confirmWorkOrder(int id) {
        require(Permission.WORK_ORDER_WRITE);
        target.confirmWorkOrder(id);
    }

    @Override
    public void startWorkOrder(int id) {
        require(Permission.WORK_ORDER_WRITE);
        target.startWorkOrder(id);
    }

    @Override
    public void completeWorkOrder(int id) {
        require(Permission.WORK_ORDER_WRITE);
        target.completeWorkOrder(id);
    }

    @Override
    public void cancelWorkOrder(int id) {
        require(Permission.WORK_ORDER_WRITE);
        target.cancelWorkOrder(id);
    }

    private void require(Permission permission) {
        if (!AccessPolicy.can(role, permission)) {
            throw new PermissionDeniedException(role, permission);
        }
    }
}
