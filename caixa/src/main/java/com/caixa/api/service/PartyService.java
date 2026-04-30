package com.caixa.api.service;

import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.core.repository.CoreAccountRepository;
import com.caixa.api.dto.Party;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyService {

    private final CoreAccountRepository coreAccountRepository;

    @Value("${fsp.id:caixa}")
    private String fspId;

    @Value("${fsp.currency:CVE}")
    private String currency;

    @Value("${sdk.outbound.url}")
    private String sdkOutboundUrl;

    public void registerParticipant(String idType, String idValue) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> entry = new HashMap<>();
        entry.put("idType", idType);
        entry.put("idValue", idValue);
        entry.put("currency", currency);

        try {
            log.info("A registar participante no ALS via SDK (CAIXA): {}/{} currency={}", idType, idValue, currency);
            restTemplate.postForEntity(sdkOutboundUrl + "/accounts", List.of(entry), Map.class);
        } catch (Exception e) {
            log.error("Erro ao registar participante no ALS (CAIXA): {}", e.getMessage());
        }
    }

    /**
     * Supported oracle types:
     *   MSISDN     — lookup by mobile number (consumer and business)
     *   ACCOUNT_ID — lookup by internal account number
     *   BUSINESS   — lookup by business registration ID
     */
    public Optional<Party> getParty(String idType, String idValue) {
        log.info("Party lookup: idType={}, idValue={}", idType, idValue);

        Optional<CoreAccount> accountOpt = switch (idType.toUpperCase()) {
            case "MSISDN"     -> coreAccountRepository.findByMsisdn(idValue);
            case "ACCOUNT_ID" -> coreAccountRepository.findByAccountNumber(idValue);
            case "BUSINESS"   -> coreAccountRepository.findByBusinessId(idValue);
            default -> {
                log.warn("Unsupported idType: {}", idType);
                yield Optional.empty();
            }
        };

        return accountOpt.map(acc -> {
            List<Map<String, String>> extensions = new ArrayList<>();
            Map<String, String> ext = new HashMap<>();
            ext.put("key", "accountNumber");
            ext.put("value", acc.getAccountNumber());
            extensions.add(ext);

            return Party.builder()
                    .idType(idType)
                    .idValue(idValue)
                    .type(acc.getPartyType() != null ? acc.getPartyType() : "CONSUMER")
                    .displayName(acc.getDisplayName())
                    .firstName(acc.getFirstName())
                    .lastName(acc.getLastName())
                    .dateOfBirth(acc.getDateOfBirth())
                    .fspId(fspId)
                    .extensionList(extensions)
                    .build();
        });
    }

    public Optional<CoreAccount> findAccountByIdType(String idType, String idValue) {
        return switch (idType.toUpperCase()) {
            case "MSISDN"     -> coreAccountRepository.findByMsisdn(idValue);
            case "ACCOUNT_ID" -> coreAccountRepository.findByAccountNumber(idValue);
            case "BUSINESS"   -> coreAccountRepository.findByBusinessId(idValue);
            default -> {
                Optional<CoreAccount> byMsisdn = coreAccountRepository.findByMsisdn(idValue);
                yield byMsisdn.isPresent() ? byMsisdn : coreAccountRepository.findByAccountNumber(idValue);
            }
        };
    }
}
