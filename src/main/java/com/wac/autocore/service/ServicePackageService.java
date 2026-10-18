package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.ServicePackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;

/**
 * <b>ServicePackageService</b>
 * <p>Ansvar: skapa, ändra, ta bort och läsa servicepaket.</p>
 * <p>Regler som kräver databasen ligger här: att tjänsterna finns och att namnet är unikt. Regler om paketet i sig ligger i ServicePackage.</p>
 */
@Validated
@Service
@Transactional
public class ServicePackageService {

    private final ServicePackageRepository packageRepository;
    private final ServiceItemRepository serviceItemRepository;

    public ServicePackageService(ServicePackageRepository packageRepository, ServiceItemRepository serviceItemRepository) {
        this.packageRepository = packageRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicePackage> findAll() {
        return packageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ServicePackage> findById(int id) {
        return packageRepository.findById(id);
    }

    public ServicePackage create(@NotBlank(message = "error.servicePackage.nameRequired") String name,
                                 String description,
                                 @NotEmpty(message = "error.servicePackage.noServices") List<@Positive Integer> serviceItemIds) {
        List<ServiceItem> services = serviceItemRepository.requireAllById(serviceItemIds);
        ServicePackage servicePackage = ServicePackage.create(name, description, services);
        if (packageRepository.existsByNameIgnoreCase(servicePackage.getName())) {
            throw new ValidationException("error.servicePackage.nameTaken", servicePackage.getName());
        }
        return packageRepository.save(servicePackage);
    }

    public ServicePackage update(@Positive @NotNull(message = "error.servicePackage.nullValue") Integer id,
                                 @NotBlank(message = "error.servicePackage.nameRequired")String name,
                                 String description,
                                 @NotEmpty(message = "error.servicePackage.noServices") List<Integer> serviceItemIds) {
        ServicePackage servicePackage = packageRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("ServicePackage", id, "error.servicePackage.notFound"));

        List<ServiceItem> services = serviceItemRepository.requireAllById(serviceItemIds);

        String newName = name != null ? name.trim() : null;
        if (newName != null && !newName.isEmpty()
                && packageRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new ValidationException("error.servicePackage.nameTaken", newName);
        }

        servicePackage.update(name, description, services);

        return packageRepository.save(servicePackage);
    }

    public void delete(@Positive @NotNull(message = "error.servicePackage.nullValue") Integer id) {
        ServicePackage servicePackage = packageRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("ServicePackage", id, "error.servicePackage.notFound"));
        packageRepository.delete(servicePackage);
    }
}
