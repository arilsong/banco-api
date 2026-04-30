package com.bca.api.core.repository;

import com.bca.api.core.model.CoreUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoreUserRepository extends JpaRepository<CoreUser, Long> {
    Optional<CoreUser> findByUsername(String username);
}
