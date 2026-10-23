package com.wac.autocore.headless;

import java.util.Collections;
import java.util.List;

/**
 * <b>ScenarioContext</b>
 * <p>Responsibility: Shared state between the steps in a scenario. A step writes a value, e.g. the id
 * of a created order, and later steps read it.</p>
 * <p>Created anew for each run of {@link DropInScenario#run()}, so runs do not affect
 * each other. Has typed fields instead of a {@code Map}, so typos are caught by the
 * compiler.</p>
 */
public final class ScenarioContext {

    private int vehicleId;
    private int mechanicId;
    private List<Integer> serviceItemIds = Collections.emptyList();

    private int workOrderId;
    private int invoiceId;
    private int cancelledOrderId;

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    public List<Integer> getServiceItemIds() {
        return serviceItemIds;
    }

    public void setServiceItemIds(List<Integer> serviceItemIds) {
        this.serviceItemIds = serviceItemIds;
    }

    public int getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(int workOrderId) {
        this.workOrderId = workOrderId;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getCancelledOrderId() {
        return cancelledOrderId;
    }

    public void setCancelledOrderId(int cancelledOrderId) {
        this.cancelledOrderId = cancelledOrderId;
    }
}
