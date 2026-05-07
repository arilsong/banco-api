package com.bca.api.service;

import com.bca.api.core.model.CoreAccount;
import com.bca.api.dto.QuoteRequest;
import com.bca.api.dto.QuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteService {

    @Value("${fee.percentage:0}")
    private BigDecimal feePercentage;

    @Value("${fee.currency:CVE}")
    private String feeCurrency;

    private final PartyService partyService;

    public QuoteResponse processQuote(QuoteRequest request) {
        QuoteRequest.Amount parsed = request.getAmountParsed();
        String currency    = parsed.getCurrency() != null ? parsed.getCurrency() : "CVE";
        String amountValue = parsed.getAmount()   != null ? parsed.getAmount()   : "0";

        if (request.getTo() != null && request.getTo().getIdValue() != null) {
            String payeeType    = request.getTo().getIdType();
            String payeeIdValue = request.getTo().getIdValue();
            CoreAccount payeeAccount = partyService.findAccountByIdType(payeeType, payeeIdValue)
                    .orElseThrow(() -> {
                        log.warn("Conta/Alias de destino não encontrado: {} {}", payeeType, payeeIdValue);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta/Alias de destino não encontrado.");
                    });
            log.info("Cliente destino verificado: {} (Account: {})", payeeAccount.getDisplayName(), payeeAccount.getAccountNumber());
        }

        BigDecimal amount = new BigDecimal(amountValue);
        BigDecimal fee    = amount.multiply(feePercentage)
                                  .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal payeeReceives = amount.subtract(fee);

        log.info("Quote: montante={} taxa={}% fee={} payeeReceives={} {}", amountValue, feePercentage, fee, payeeReceives, currency);

        return QuoteResponse.builder()
                .quoteId(request.getQuoteId())
                .transactionId(request.getTransactionId())
                .transferAmount(amountValue)
                .transferAmountCurrency(currency)
                .payeeReceiveAmount(payeeReceives.toPlainString())
                .payeeReceiveAmountCurrency(currency)
                .payeeFspFee(fee.toPlainString())
                .payeeFspFeeCurrency(feeCurrency)
                .payeeFspCommission("0")
                .payeeFspCommissionCurrency(currency)
                .expiration(request.getExpiration())
                .build();
    }
}
