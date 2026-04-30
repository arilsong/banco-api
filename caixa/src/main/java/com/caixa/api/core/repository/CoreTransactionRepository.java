package com.caixa.api.core.repository;

import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.core.model.CoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoreTransactionRepository extends JpaRepository<CoreTransaction, Long> {
    List<CoreTransaction> findByAccountOrderByTimestampDesc(CoreAccount account);
}
