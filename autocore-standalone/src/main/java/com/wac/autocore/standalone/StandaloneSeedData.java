package com.wac.autocore.standalone;

import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;

/**
 * <b>StandaloneSeedData</b>
 * <p>Responsibility: Fills the in-memory repositories with the data the scenarios need:
 * a VIP customer with one vehicle, one mechanic and two service items.</p>
 * <p>Saves straight to the repositories, because the mechanic and service catalog
 * have no create operations in their services.</p>
 */
final class StandaloneSeedData {

    private StandaloneSeedData() {
    }

    static void seed(CustomerRepo customers, VehicleRepo vehicles,
                     MechanicRepository mechanics, ServiceItemRepository serviceItems) {
        Customer vip = new Customer("Tomas Wigell", "070-1234567", "tomas@wigell.se");
        vip.setVip(true);
        customers.save(vip);

        vehicles.save(new Vehicle("ABC123", "Volvo", "V60", 2020, vip.getId()));
        mechanics.save(new Mechanic("Carl Gustav", "070-9876543", "Engine calibration"));
        serviceItems.save(new ServiceItem("Oil change", "Engine oil and filter", Money.of("1200.00"), 60));
        serviceItems.save(new ServiceItem("Diagnostics", "Fault code reading", Money.of("800.00"), 45));
    }
}