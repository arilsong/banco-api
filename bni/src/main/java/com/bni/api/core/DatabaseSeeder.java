package com.bni.api.core;

import com.bni.api.core.model.CoreAccount;
import com.bni.api.core.model.CoreTransaction;
import com.bni.api.core.model.CoreUser;
import com.bni.api.core.repository.CoreAccountRepository;
import com.bni.api.core.repository.CoreTransactionRepository;
import com.bni.api.core.repository.CoreUserRepository;
import com.bni.api.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final CoreAccountRepository coreAccountRepository;
    private final CoreUserRepository coreUserRepository;
    private final CoreTransactionRepository coreTransactionRepository;
    private final PartyService partyService;

    @Override
    public void run(String... args) throws Exception {
        if (coreUserRepository.count() == 0) {
            log.info("A iniciar a população do core banking BNI...");

            CoreUser user1 = coreUserRepository.save(CoreUser.builder()
                    .username("joao")
                    .password("password")
                    .displayName("João Costa")
                    .build());

            CoreUser user2 = coreUserRepository.save(CoreUser.builder()
                    .username("ana")
                    .password("password")
                    .displayName("Ana Pereira")
                    .build());

            coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("DO0001")
                    .msisdn("3519101234")
                    .partyType("CONSUMER")
                    .displayName("João Costa")
                    .firstName("João")
                    .lastName("Costa")
                    .dateOfBirth("1988-03-15")
                    .balance(new BigDecimal("4500.00"))
                    .currency("EUR")
                    .user(user1)
                    .build());

            partyService.registerParticipant("MSISDN", "3519101234");

            CoreAccount ana = coreAccountRepository.save(CoreAccount.builder()
                    .accountNumber("DO0002")
                    .msisdn("3519205678")
                    .partyType("CONSUMER")
                    .displayName("Ana Pereira")
                    .firstName("Ana")
                    .lastName("Pereira")
                    .dateOfBirth("1993-07-22")
                    .balance(new BigDecimal("8200.00"))
                    .currency("EUR")
                    .user(user2)
                    .build());

            partyService.registerParticipant("MSISDN", "3519205678");

            coreTransactionRepository.save(CoreTransaction.builder()
                    .transactionId("BNI-SEED-001")
                    .description("Depósito de Abertura")
                    .amount(new BigDecimal("4500.00"))
                    .currency("EUR")
                    .timestamp(LocalDateTime.now().minusDays(10))
                    .account(ana)
                    .build());

            log.info("Base de dados BNI populada: 2 contas CONSUMER em EUR.");
        }
    }
}
