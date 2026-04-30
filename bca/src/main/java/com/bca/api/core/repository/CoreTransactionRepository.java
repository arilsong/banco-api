package com.bca.api.core.repository;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.model.CoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoreTransactionRepository extends JpaRepository<CoreTransaction, Long> {
    List<CoreTransaction> findByAccountOrderByTimestampDesc(CoreAccount account);
}
