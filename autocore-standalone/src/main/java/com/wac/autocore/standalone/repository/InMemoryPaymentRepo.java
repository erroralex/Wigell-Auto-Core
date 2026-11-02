package com.wac.autocore.standalone.repository;

import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.PaymentRepo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link PaymentRepo} for running AutoCore without a database.
 */
public class InMemoryPaymentRepo implements PaymentRepo {

    private final Map<Integer, Payment> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public List<Payment> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Payment> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public <S extends Payment> S save(S payment) {
        if (InMemoryIds.idOf(payment) == 0) {
            InMemoryIds.setId(payment, ids.next());
        }
        store.put(InMemoryIds.idOf(payment), payment);
        return payment;
    }
}
