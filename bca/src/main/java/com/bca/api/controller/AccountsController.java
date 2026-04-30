package com.bca.api.controller;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.core.repository.CoreAccountRepository;
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

