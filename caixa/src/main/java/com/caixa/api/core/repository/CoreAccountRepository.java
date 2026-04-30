package com.caixa.api.core.repository;

import com.caixa.api.core.model.CoreAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoreAccountRepository extends JpaRepository<CoreAccount, Long> {

    Optional<CoreAccount> findByMsisdn(String msisdn);
    Optional<CoreAccount> findByAccountNumber(String accountNumber);
    Optional<CoreAccount> findByBusinessId(String businessId);
}
