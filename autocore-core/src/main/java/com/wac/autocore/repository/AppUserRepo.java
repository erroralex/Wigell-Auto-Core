package com.wac.autocore.repository;

import com.wac.autocore.model.AppUser;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface AppUserRepo extends org.springframework.data.repository.Repository<AppUser, Integer> {
    Optional<AppUser> findById(Integer id);
    Optional<AppUser> findByUsername(String username);
}
