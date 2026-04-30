package com.caixa.api.service;
import com.caixa.api.core.model.CoreAccount;
import com.caixa.api.dto.QuoteRequest;
import com.caixa.api.dto.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {
    private final PartyService partyService;

    public QuoteResponse processQuote(QuoteRequest request) {
        QuoteRequest.Amount parsed = request.getAmountParsed();
        String currency = parsed.getCurrency() != null ? parsed.getCurrency() : "CVE";
        String amountValue = parsed.getAmount() != null ? parsed.getAmount() : "0";

        if (request.getTo() != null && request.getTo().getIdValue() != null) {
            String payeeType = request.getTo().getIdType();
            String payeeIdValue = request.getTo().getIdValue();
            CoreAccount payeeAccount = partyService.findAccountByIdType(payeeType, payeeIdValue)
                    .orElseThrow(() -> {
                        log.warn("Conta/Alias de destino não encontrado: {} {}", payeeType, payeeIdValue);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta/Alias de destino não encontrado.");
                    });
            log.info("Cliente destino verificado: {} (Account: {})", payeeAccount.getDisplayName(), payeeAccount.getAccountNumber());
        }

        return QuoteResponse.builder()
                .quoteId(request.getQuoteId())
                .transactionId(request.getTransactionId())
                .transferAmount(amountValue)
                .transferAmountCurrency(currency)
                .payeeReceiveAmount(amountValue)
                .payeeReceiveAmountCurrency(currency)
                .payeeFspFee("0")
                .payeeFspFeeCurrency(currency)
                .payeeFspCommission("0")
                .payeeFspCommissionCurrency(currency)
                .expiration(request.getExpiration())
                .build();
    }
}

