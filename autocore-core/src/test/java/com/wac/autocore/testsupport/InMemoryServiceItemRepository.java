package com.wac.autocore.testsupport;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.ServiceItemRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryServiceItemRepository implements ServiceItemRepository {

    private final Map<Integer, ServiceItem> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public List<ServiceItem> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<ServiceItem> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public ServiceItem save(ServiceItem serviceItem) {
        if (InMemoryIds.idOf(serviceItem) == 0) {
            InMemoryIds.setId(serviceItem, ids.next());
        }
        store.put(InMemoryIds.idOf(serviceItem), serviceItem);
        return serviceItem;
    }
}