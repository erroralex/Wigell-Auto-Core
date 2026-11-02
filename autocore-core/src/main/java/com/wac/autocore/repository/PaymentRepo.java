package com.wac.autocore.repository;

import com.wac.autocore.model.Payment;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface PaymentRepo extends org.springframework.data.repository.Repository<Payment, Integer> {
    List<Payment> findAll();
    Optional<Payment> findById(Integer id);
    <S extends Payment> S save(S payment);
}