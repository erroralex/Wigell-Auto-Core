package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import javax.persistence.*;
import java.util.*;

/**
 * <b>ServicePackage</b>
 * <p>Ansvar: ett namngivet urval av katalogtjänster som används som mall när en bokning skapas.</p>
 * <p>Viktigt: paketet pekar på levande ServiceItem. En bokning kopierar tjänsterna till egna snapshot-rader (BookingServiceItem.snapshotOf) och refererar aldrig till paketet, så en ändring i ett paket påverkar aldrig befintliga bokningar.</p>
 */
@Entity
@Table(name = "service_package")
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "service_package_item",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "service_item_id")
    )
    private Set<ServiceItem> services = new LinkedHashSet<>();

    protected ServicePackage() {
    }

    private ServicePackage(String name, String description, Collection<ServiceItem> services) {
        this.name = name;
        this.description = description;
        this.services.addAll(services);
    }

    public static ServicePackage create(String name, String description, Collection<ServiceItem> services) {
        validate(name, services);
        return new ServicePackage(name.trim(), description, services);
    }

    public void update(String name, String description, Collection<ServiceItem> services) {
        validate(name, services);
        List<ServiceItem> newServices = new ArrayList<>(services);
        this.name = name.trim();
        this.description = description;
        this.services.clear();
        this.services.addAll(newServices);
    }

    private static void validate(String name, Collection<ServiceItem> services) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("error.servicePackage.nameRequired");
        }
        if (services == null || services.isEmpty()) {
            throw new ValidationException("error.servicePackage.noServices");
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<ServiceItem> getServices() {
        return Collections.unmodifiableSet(services);
    }

    @Override
    public String toString() {
        return
                "ServicePackage " + name + " (" + services.size() + " services)";
    }
}
