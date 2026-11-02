package com.wac.autocore.standalone.repository;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.InvoiceRepo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link InvoiceRepo} for running AutoCore without a database.
 */
public class InMemoryInvoiceRepo implements InvoiceRepo {

    private final Map<Integer, Invoice> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public <S extends Invoice> S save(S invoice) {
        if (InMemoryIds.idOf(invoice) == 0) {
            InMemoryIds.setId(invoice, ids.next());
        }
        store.put(InMemoryIds.idOf(invoice), invoice);
        return invoice;
    }

    @Override
    public boolean existsByWorkOrderId(int workOrderId) {
        return store.values().stream()
                .anyMatch(i -> i.getWorkOrderId() == workOrderId);
    }

    @Override
    public List<Invoice> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Invoice> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }
}
