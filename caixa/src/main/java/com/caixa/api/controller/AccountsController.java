package com.caixa.api.controller;

import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.core.repository.CoreAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountsController {

    private final CoreAccountRepository accountRepository;

    @GetMapping("/{msisdn}")
    public ResponseEntity<CoreAccount> getAccountInfo(@PathVariable String msisdn) {
        return accountRepository.findByMsisdn(msisdn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

