package com.bca.api.controller;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.model.CoreUser;
import com.bca.api.core.repository.CoreAccountRepository;
import com.bca.api.core.repository.CoreUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalApiController {

    private final CoreUserRepository coreUserRepository;
    private final CoreAccountRepository coreAccountRepository;
    private final com.bca.api.core.repository.CoreTransactionRepository coreTransactionRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        log.info("Tentativa de login interno para usuário: {}", username);

        Optional<CoreUser> userOpt = coreUserRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            log.info("Login interno bem-sucedido para: {}", username);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("userId", userOpt.get().getId().toString());
            return ResponseEntity.ok(response);
        }

        log.warn("Falha no login interno para: {}", username);
        return ResponseEntity.ok(Map.of("valid", false));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> listAccounts(@RequestParam("user") Long userId) {
        log.info("Listando contas para o userId interno: {}", userId);

        List<CoreAccount> accounts = coreAccountRepository.findAll().stream()
                .filter(acc -> acc.getUser() != null && acc.getUser().getId().equals(userId))
                .collect(Collectors.toList());

        List<Map<String, Object>> response = accounts.stream()
                .map(acc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", acc.getAccountNumber());
                    map.put("msisdn", acc.getMsisdn());
                    // Tipo da conta derivado do número de conta
                    String accountType = acc.getAccountNumber().startsWith("DO") ? "CURRENT" : "SAVINGS";
                    map.put("type", accountType);
                    map.put("accountType", accountType);

                    // Nome da conta: displayName prioritário, senão firstName + lastName
                    String accountName = acc.getDisplayName() != null && !acc.getDisplayName().isBlank()
                            ? acc.getDisplayName()
                            : ((acc.getFirstName() != null ? acc.getFirstName() : "") + " "
                               + (acc.getLastName() != null ? acc.getLastName() : "")).trim();
                    map.put("accountName", accountName.isBlank() ? acc.getAccountNumber() : accountName);

                    map.put("balance", acc.getBalance());
                    map.put("currency", acc.getCurrency());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String accountId) {
        log.info("Consultando saldo para conta interna: {}", accountId);

        Optional<CoreAccount> accountOpt = coreAccountRepository.findAll().stream()
                .filter(acc -> acc.getAccountNumber().equals(accountId))
                .findFirst();

        if (accountOpt.isPresent()) {
            CoreAccount acc = accountOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("accountId", acc.getAccountNumber());
            response.put("balance", acc.getBalance());
            response.put("currency", acc.getCurrency());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<?> getTransactions(@PathVariable String accountId) {
        log.info("Consultando histórico de transações para conta (BCA): {}", accountId);

        Optional<CoreAccount> accountOpt = coreAccountRepository.findAll().stream()
                .filter(acc -> acc.getAccountNumber().equals(accountId))
                .findFirst();

        if (accountOpt.isPresent()) {
            List<com.bca.api.core.model.CoreTransaction> transactions = coreTransactionRepository.findByAccountOrderByTimestampDesc(accountOpt.get());
            
            List<Map<String, Object>> response = transactions.stream()
                    .map(tx -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", tx.getTransactionId());
                        map.put("description", tx.getDescription());
                        map.put("amount", tx.getAmount());
                        map.put("currency", tx.getCurrency());
                        map.put("date", tx.getTimestamp().toString());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }
}
