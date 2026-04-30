package com.bca.api.core;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.model.CoreUser;
import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.core.repository.CoreUserRepository;
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
    private final com.bca.api.core.repository.CoreTransactionRepository coreTransactionRepository;
    private final com.bca.api.service.PartyService sdkClient;

    @Override
    public void run(String... args) throws Exception {
        if (coreUserRepository.count() == 0) {
            log.info("A iniciar a população do core banking BCA (H2 Database)...");

            // --- Utilizadores ---
            CoreUser user1 = coreUserRepository.save(CoreUser.builder()
                    .username("alvaro")
                    .password("123456")
                    .displayName("Alvaro Silva")
                    .build());

            CoreUser user2 = coreUserRepository.save(CoreUser.builder()
                    .username("maria")
                    .password("password")
                    .displayName("Maria Santos")
                    .build());

            CoreUser userBusiness = coreUserRepository.save(CoreUser.builder()
                    .username("mercadocentral")
                    .password("business123")
                    .displayName("Mercado Central CV")
                    .build());

            // --- Contas CONSUMER ---
            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("DO0001")
                    .msisdn("2389512347")
                    .partyType("CONSUMER")
                    .displayName("Alvaro Silva - Principal")
                    .firstName("Alvaro")
                    .lastName("Silva")
                    .dateOfBirth("1990-01-01")
                    .balance(new BigDecimal("50000.00"))
                    .currency("CVE")
                    .user(user1)
                    .build());

            sdkClient.registerParticipant("MSISDN", "2389512347");

            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("PO0001")

                    .msisdn("2389512347")
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
                    .msisdn("2389389274")
                    .partyType("CONSUMER")
                    .displayName("Maria Santos")
                    .firstName("Maria")
                    .lastName("Santos")
                    .dateOfBirth("1985-05-12")
                    .balance(new BigDecimal("30000.00"))
                    .currency("CVE")
                    .user(user2)
                    .build());

            sdkClient.registerParticipant("MSISDN", "2389389274");

            // --- Conta BUSINESS ---
            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("EM0001")
                    .msisdn("2389718001")
                    .partyType("BUSINESS")
                    .businessId("000001")
                    .displayName("Mercado Central CV")
                    .firstName("Mercado Central")
                    .lastName("de Cabo Verde Lda")
                    .dateOfBirth(null)
                    .balance(new BigDecimal("500000.00"))
                    .currency("CVE")
                    .user(userBusiness)
                    .build());

            // --- Histórico de transações ---
            CoreAccount alvaroAcc = coreAccountRepository.findByAccountNumber("DO0001").get();

            coreTransactionRepository.save(com.bca.api.core.model.CoreTransaction.builder()
                    .transactionId("BCA-SEED-001")
                    .description("Depósito de Abertura")
                    .amount(new BigDecimal("50000.00"))
                    .currency("CVE")
                    .timestamp(java.time.LocalDateTime.now().minusDays(5))
                    .account(alvaroAcc)
                    .build());

            coreTransactionRepository.save(com.bca.api.core.model.CoreTransaction.builder()
                    .transactionId("BCA-SEED-002")
                    .description("Compra Restaurante")
                    .amount(new BigDecimal("-3200.00"))
                    .currency("CVE")
                    .timestamp(java.time.LocalDateTime.now().minusDays(2))
                    .account(alvaroAcc)
                    .build());

            log.info("Base de dados BCA populada: 3 contas CONSUMER + 1 conta BUSINESS.");
        }
    }
}
