package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepo;

    public ServiceItemService (ServiceItemRepository serviceItemRepo) {
        this.serviceItemRepo = serviceItemRepo;
    }

    public List<ServiceItem> listAll() {
        return serviceItemRepo.findAll();
    }

    @Transactional
    public ServiceItem updatePrice(int id, double newPrice) {
        if (newPrice <= 0) {
            throw new ValidationException("error.serviceItem.invalidPrice");
        }
        ServiceItem item = serviceItemRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceItem.notFound"));
        item.setPrice(newPrice);
        return serviceItemRepo.save(item);
    }

}
