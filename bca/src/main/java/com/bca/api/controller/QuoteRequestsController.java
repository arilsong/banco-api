package com.bca.api.controller;


import com.bca.api.dto.QuoteRequest;
import com.bca.api.dto.QuoteResponse;
import com.bca.api.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quoterequests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuoteRequestsController {

    private final QuoteService quoteService;

    @PostMapping
    public ResponseEntity<QuoteResponse> createQuoteRequest(@RequestBody QuoteRequest request) {
        QuoteResponse response = quoteService.processQuote(request);
        return ResponseEntity.ok(response);
    }
}

