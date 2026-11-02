package com.wac.autocore.repository;

import com.wac.autocore.model.ServicePackage;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ServicePackageRepository extends org.springframework.data.repository.Repository<ServicePackage, Integer> {
    List<ServicePackage> findAll();
    Optional<ServicePackage> findById(Integer id);
    ServicePackage save(ServicePackage servicePackage);

    void delete(ServicePackage servicePackage);

    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, int id);
}