package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;

@Validated
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
    public ServiceItem updatePrice(@Positive @NotNull(message="error.serviceItem.serviceItemNull") Integer id, Money newPrice) {
        ServiceItem item = serviceItemRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceItem.notFound"));
        item.changePrice(newPrice);
        return serviceItemRepo.save(item);
    }

}
