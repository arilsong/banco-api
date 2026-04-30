package com.bca.api.controller;

import com.bca.api.dto.Party;
import com.bca.api.service.PartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/parties")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PartiesController {

    private final PartyService partyService;


    @PostMapping("/participants")
    public ResponseEntity<?> registerParticipant(@RequestBody Map<String, String> body) {
        String idType  = body.get("idType");
        String idValue = body.get("idValue");
        if (idType == null || idValue == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "idType and idValue are required"));
        }
        partyService.registerParticipant(idType, idValue);
        return ResponseEntity.ok(Map.of("status", "registered", "idType", idType, "idValue", idValue));
    }


    /**
     * Supports three oracle types:
     *   GET /parties/MSISDN/{phoneNumber}
     *   GET /parties/ACCOUNT_ID/{accountNumber}
     *   GET /parties/BUSINESS/{businessId}
     */
    @GetMapping("/{idType}/{idValue}")
    public ResponseEntity<?> getParty(
            @PathVariable String idType,
            @PathVariable String idValue) {

        Optional<Party> party = partyService.getParty(idType, idValue);

        if (party.isEmpty()) return ResponseEntity.notFound().build();

        Party p = party.get();

        Map<String, Object> response = new HashMap<>();
        response.put("idType", p.getIdType());
        response.put("idValue", p.getIdValue());
        response.put("type", p.getType());          // dynamic: CONSUMER or BUSINESS
        response.put("displayName", p.getDisplayName());
        response.put("firstName", p.getFirstName());
        response.put("middleName", "");
        response.put("lastName", p.getLastName());
        response.put("dateOfBirth", p.getDateOfBirth());
        response.put("merchantClassificationCode", "BUSINESS".equals(p.getType()) ? "0001" : "1234");

        return ResponseEntity.ok(response);
    }
}
