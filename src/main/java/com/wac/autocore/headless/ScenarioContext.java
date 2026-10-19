package com.wac.autocore.headless;

import java.util.Collections;
import java.util.List;

/**
 * <b>ScenarioContext</b>
 * <p>Ansvar: Delat tillstånd mellan stegen i ett scenario. Ett steg skriver ett värde, t.ex. id:t
 * för en skapad order, och senare steg läser det.</p>
 * <p>Skapas på nytt för varje körning av {@link DropInScenario#run()}, så körningar påverkar
 * inte varandra. Har typade fält i stället för en {@code Map}, så att stavfel upptäcks av
 * kompilatorn.</p>
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
