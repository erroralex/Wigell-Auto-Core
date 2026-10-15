package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.Positive;

@Repository
public interface InvoiceRepo extends JpaRepository<Invoice, Integer> {
    boolean existsByWorkOrderId(@Positive int workOrderId);
}
