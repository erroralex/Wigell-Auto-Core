package com.wac.autocore.repository;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.ServiceItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ServiceItemRepository extends org.springframework.data.repository.Repository<ServiceItem, Integer> {

    List<ServiceItem> findAll();
    Optional<ServiceItem> findById(Integer id);
    ServiceItem save(ServiceItem serviceItem);

    // The only place that looks up a selected service; an unknown id gives the same error key everywhere.
    default ServiceItem requireById(int id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound"));
    }

    // Looks up selected services in the selected order. A missing list becomes empty, so the domain decides whether services are required.
    // Duplicates are not removed here: the domain (ServiceLines, ServicePackage) owns that rule.
    default List<ServiceItem> requireAllById(List<Integer> ids) {
        List<ServiceItem> services = new ArrayList<>();
        if (ids != null) {
            for (int id : ids) {
                services.add(requireById(id));
            }
        }
        return services;
    }
}