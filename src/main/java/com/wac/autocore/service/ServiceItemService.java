package com.wac.autocore.service;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;

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


}
