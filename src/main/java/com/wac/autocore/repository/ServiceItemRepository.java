package com.wac.autocore.repository;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Integer> {

    // Enda platsen som slår upp en vald tjänst; ett okänt id ger samma felnyckel överallt.
    default ServiceItem requireById(int id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound"));
    }

    // Slår upp valda tjänster i vald ordning. En saknad lista blir tom, så att domänen avgör om tjänster krävs.
    // Dubbletter tas inte bort här: domänen (ServiceLines, ServicePackage) äger den regeln.
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
