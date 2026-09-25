package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class GarageSystem {



    public void showCustomers() {
        System.out.println();
        System.out.println("=== CUSTOMERS ===");

        if (Database.getCustomers().isEmpty()) {
            System.out.println("No customers found.");
            return;
        }

        for (Customer customer : Database.getCustomers()) {
            System.out.println(customer);
        }
    }

    public void showVehicles() {
        System.out.println();
        System.out.println("=== VEHICLES ===");

        if (Database.getVehicles().isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }

        for (Vehicle vehicle : Database.getVehicles()) {
            System.out.println(vehicle);
        }
    }

    public void showBookings() {
        System.out.println();
        System.out.println("=== BOOKINGS ===");

        if (Database.getBookings().isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }

        for (Booking booking : Database.getBookings()) {
            System.out.println(booking);
        }
    }

    public void showServiceItems() {
        System.out.println();
        System.out.println("=== SERVICES ===");

        if (Database.getServiceItems().isEmpty()) {
            System.out.println("No services found.");
            return;
        }

        for (ServiceItem serviceItem : Database.getServiceItems()) {
            System.out.println(serviceItem);
        }
    }

    public void showMechanics() {
        System.out.println();
        System.out.println("=== MECHANICS ===");

        if (Database.getMechanics().isEmpty()) {
            System.out.println("No mechanics found.");
            return;
        }

        for (Mechanic mechanic : Database.getMechanics()) {
            System.out.println(mechanic);
        }
    }



    public void showInvoices() {
        System.out.println();
        System.out.println("=== INVOICES ===");

        if (Database.getInvoices().isEmpty()) {
            System.out.println("No invoices found.");
            return;
        }

        for (Invoice invoice : Database.getInvoices()) {
            System.out.println(invoice);
        }
    }

    public void showPayments() {
        System.out.println();
        System.out.println("=== PAYMENTS ===");

        if (Database.getPayments().isEmpty()) {
            System.out.println("No payments found.");
            return;
        }

        for (Payment payment : Database.getPayments()) {
            System.out.println(payment);
        }
    }

    public Customer createCustomer(String name, String phone, String email) {
        int id = Database.getCustomers().size() + 1;

        Customer customer = new Customer(id, name, phone, email);
        Database.getCustomers().add(customer);

        System.out.println("Customer created successfully.");
        System.out.println(customer);

        return customer;
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {

        Customer customer = findCustomer(customerId);

        if (customer == null) {
            System.out.println("Customer with ID " + customerId + " does not exist.");
            return null;
        }

        int id = Database.getVehicles().size() + 1;

        Vehicle vehicle = new Vehicle(
                id,
                registrationNumber,
                brand,
                model,
                year,
                customerId
        );

        Database.getVehicles().add(vehicle);

        System.out.println("Vehicle created successfully.");
        System.out.println(vehicle);

        return vehicle;
    }

    /*public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description) {

        Vehicle vehicle = findVehicle(vehicleId);

        if (vehicle == null) {
            System.out.println("Vehicle with ID " + vehicleId + " does not exist.");
            return null;
        }

        int id = Database.getBookings().size() + 1;

        Booking booking = new Booking(
                id,
                vehicleId,
                date,
                description
        );

        Database.getBookings().add(booking);

        System.out.println("Booking created successfully.");
        System.out.println(booking);

        return booking;
    }*/

    public Payment processPayment(int invoiceId, String paymentType) {
        Invoice invoice = findInvoice(invoiceId);

        if (invoice == null) {
            System.out.println("Invoice with ID " + invoiceId + " does not exist.");
            return null;
        }

        if (invoice.isPaid()) {
            System.out.println("Invoice has already been paid.");
            return null;
        }

        int id = Database.getPayments().size() + 1;

        Payment payment = new Payment(
                id,
                invoiceId,
                invoice.getTotalAmount(),
                paymentType
        );

        boolean successful = false;

        if (paymentType.equalsIgnoreCase("CARD")) {

            System.out.println("Connecting directly to SuperCardPayment...");
            System.out.println("Card payment approved.");
            successful = true;

        } else if (paymentType.equalsIgnoreCase("SWISH")) {

            System.out.println("Calling Swish payment service...");
            System.out.println("Swish payment approved.");
            successful = true;

        } else if (paymentType.equalsIgnoreCase("CASH")) {

            System.out.println("Registering cash payment...");
            successful = true;

        } else {

            System.out.println("Unknown payment type.");
        }

        payment.setSuccessful(successful);
        Database.getPayments().add(payment);

        if (successful) {
            invoice.setPaid(true);

            System.out.println("Payment completed successfully.");
            System.out.println("Sending payment confirmation to customer...");
            System.out.println("Confirmation sent.");
        } else {
            System.out.println("Payment failed.");
        }

        return payment;
    }

    private Customer findCustomer(int id) {
        for (Customer customer : Database.getCustomers()) {
            if (customer.getId() == id) {
                return customer;
            }
        }

        return null;
    }

    private Vehicle findVehicle(int id) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == id) {
                return vehicle;
            }
        }

        return null;
    }

    private Booking findBooking(int id) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == id) {
                return booking;
            }
        }

        return null;
    }

    private Mechanic findMechanic(int id) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == id) {
                return mechanic;
            }
        }

        return null;
    }

    private ServiceItem findServiceItem(int id) {
        for (ServiceItem serviceItem : Database.getServiceItems()) {
            if (serviceItem.getId() == id) {
                return serviceItem;
            }
        }

        return null;
    }

    private Invoice findInvoice(int id) {
        for (Invoice invoice : Database.getInvoices()) {
            if (invoice.getId() == id) {
                return invoice;
            }
        }

        return null;
    }
}