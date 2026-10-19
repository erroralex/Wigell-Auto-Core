package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface InvoiceRepo extends org.springframework.data.repository.Repository<Invoice, Integer> {
    List<Invoice> findAll();
    Optional<Invoice> findById(Integer id);
    <S extends Invoice> S save(S invoice);
    boolean existsByWorkOrderId(int workOrderId);
}