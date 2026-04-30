package com.caixa.api.core;

import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.core.model.CoreUser;
import com.caixa.api.core.repository.CoreAccountRepository;
import com.caixa.api.core.repository.CoreUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final CoreAccountRepository coreAccountRepository;
    private final CoreUserRepository coreUserRepository;
    private final com.caixa.api.core.repository.CoreTransactionRepository coreTransactionRepository;
    private final com.caixa.api.service.PartyService sdkClient;

    @Override
    public void run(String... args) throws Exception {
        if (coreUserRepository.count() == 0) {
            log.info("A iniciar a população do core banking Caixa (H2 Database)...");

            // --- Utilizadores ---
            CoreUser user1 = coreUserRepository.save(CoreUser.builder()
                    .username("alvaro")
                    .password("123456")
                    .displayName("Alvaro Silva")
                    .build());

            CoreUser user2 = coreUserRepository.save(CoreUser.builder()
                    .username("carlos")
                    .password("password")
                    .displayName("Carlos Fonseca")
                    .build());

            CoreUser userBusiness = coreUserRepository.save(CoreUser.builder()
                    .username("lojaboavista")
                    .password("business123")
                    .displayName("Loja Boa Vista")
                    .build());

            // --- Contas CONSUMER ---
            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("DO0001")
                    .msisdn("2389634521")
                    .partyType("CONSUMER")
                    .displayName("Alvaro Silva - Principal")
                    .firstName("Alvaro")
                    .lastName("Silva")
                    .dateOfBirth("1990-01-01")
                    .balance(new BigDecimal("50000.00"))
                    .currency("CVE")
                    .user(user1)
                    .build());

            sdkClient.registerParticipant("MSISDN", "2389634521");

            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("PO0001")
                    .msisdn("2389634521")
                    .partyType("CONSUMER")
                    .displayName("Alvaro Silva - Poupança")
                    .firstName("Alvaro")
                    .lastName("Silva")
                    .dateOfBirth("1990-01-01")
                    .balance(new BigDecimal("1500.00"))
                    .currency("CVE")
                    .user(user1)
                    .build());

            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("DO0002")
                    .msisdn("2389389275")
                    .partyType("CONSUMER")
                    .displayName("Carlos Fonseca")
                    .firstName("Carlos")
                    .lastName("Fonseca")
                    .dateOfBirth("1985-05-12")
                    .balance(new BigDecimal("30000.00"))
                    .currency("CVE")
                    .user(user2)
                    .build());

            sdkClient.registerParticipant("MSISDN", "2389389275");

            // --- Conta BUSINESS ---
            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("EM0001")
                    .msisdn("2389823456")
                    .partyType("BUSINESS")
                    .businessId("000002")
                    .displayName("Loja Boa Vista")
                    .firstName("Loja Boa Vista")
                    .lastName("Lda")
                    .dateOfBirth(null)
                    .balance(new BigDecimal("350000.00"))
                    .currency("CVE")
                    .user(userBusiness)
                    .build());

            // --- Histórico de transações ---
            CoreAccount alvaroAcc = coreAccountRepository.findByAccountNumber("DO0001").get();

            coreTransactionRepository.save(com.caixa.api.core.model.CoreTransaction.builder()
                    .transactionId("SEED-001")
                    .description("Depósito Inicial")
                    .amount(new BigDecimal("50000.00"))
                    .currency("CVE")
                    .timestamp(java.time.LocalDateTime.now().minusDays(2))
                    .account(alvaroAcc)
                    .build());

            coreTransactionRepository.save(com.caixa.api.core.model.CoreTransaction.builder()
                    .transactionId("SEED-002")
                    .description("Pagamento Supermercado")
                    .amount(new BigDecimal("-4500.00"))
                    .currency("CVE")
                    .timestamp(java.time.LocalDateTime.now().minusDays(1))
                    .account(alvaroAcc)
                    .build());

            log.info("Base de dados Caixa populada: 3 contas CONSUMER + 1 conta BUSINESS.");
        }
    }
}
